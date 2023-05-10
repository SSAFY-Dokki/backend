package com.dokki.book.service;


import com.dokki.book.client.TimerClient;
import com.dokki.book.config.exception.CustomException;
import com.dokki.book.dto.UserBookInfoDto;
import com.dokki.book.dto.request.BookCompleteRequestDto;
import com.dokki.book.entity.BookEntity;
import com.dokki.book.entity.BookStatusEntity;
import com.dokki.book.repository.BookStatisticsRepository;
import com.dokki.book.repository.BookStatusRepository;
import com.dokki.util.book.dto.request.BookCompleteDirectRequestDto;
import com.dokki.util.common.error.ErrorCode;
import com.dokki.util.timer.dto.response.TimerSimpleResponseDto;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;


@Slf4j
@Service
@RequiredArgsConstructor
public class BookStatusService {

	private final String STATUS_IN_PROGRESS = "T";
	private final String STATUS_DONE = "F";

	private final BookService bookService;
	private final BookStatusRepository bookStatusRepository;
	private final BookStatisticsRepository bookStatisticRepository;
	private final BookmarkService bookmarkService;

	private final TimerClient timerClient;


	/**
	 * 타이머에 도서 추가
	 */
	public void createBookToTimer(Long userId, String bookId) {
		BookStatusEntity statusEntity = getStatusByUserIdAndBookId(userId, bookId);

		if (statusEntity == null) {
			createStatus(userId, bookId, STATUS_IN_PROGRESS);
			bookStatisticRepository.updateAddOneReadingUser(bookId);
		} else {
			modifyStatusToInprogress(userId, statusEntity);
		}
	}


	/**
	 * 책 완독 추가
	 */
	@Transactional
	public void createPastBookDone(Long userId, BookCompleteRequestDto dto) {
		BookEntity bookEntity = bookService.getBookReferenceIfExist(dto.getBookId());
		BookStatusEntity result = bookStatusRepository.findTopByUserIdAndBookId(userId, bookEntity);
		if (result != null) {
			throw new CustomException(ErrorCode.DUPLICATE_RESOURCE);
		}

		BookStatusEntity bookStatusEntity = createStatus(userId, dto.getBookId(), STATUS_DONE);

		// feign client exception 발생시 완독 추가 불가하므로 catch 하지 않음
		timerClient.directComplete(BookCompleteDirectRequestDto.builder()
			.bookId(dto.getBookId())
			.bookStatusId(bookStatusEntity.getId())
			.startTime(dto.getStartTime())
			.endTime(dto.getEndTime())
			.build());

		// 책 통계에 읽은사람 추가
		bookStatisticRepository.updateAddOneCompleteUser(bookEntity.getId());
	}


	/**
	 * 도서 상태 추가
	 *
	 * @param userId 유저 id
	 * @param bookId 책 id
	 */
	@Transactional
	public BookStatusEntity createStatus(Long userId, String bookId, String status) {
		return bookStatusRepository.save(BookStatusEntity.builder()
			.userId(userId)
			.bookId(bookService.getBookReferenceIfExist(bookId))
			.status(status)
			.build());
	}


	/**
	 * 도서 상태 변경
	 * ⇒ 완독(컬렉션) → 진행중(타이머)
	 */
	public void modifyStatusToInprogress(Long userId, BookStatusEntity statusEntity) {

		// 로그인한 유저의 책이 맞는지 확인
		if (!Objects.equals(statusEntity.getUserId(), userId)) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}

		// 상태 변경
		statusEntity.setStatus(STATUS_IN_PROGRESS);
		bookStatusRepository.save(statusEntity);

		try {
			List<TimerSimpleResponseDto> accumTimeList = timerClient.getAccumTime(List.of(statusEntity.getId()));
			// TODO : 이후 수정하기 - 책 읽은 시간 10페이지 1분 기준으로 통계 반영
			if(!accumTimeList.isEmpty() && accumTimeList.get(0).getAccumTime() > 0){
				bookStatisticRepository.updateReadDatasReading(statusEntity.getBookId().getId(), accumTimeList.get(0).getAccumTime());
			}
		} catch (FeignException e){
			log.error(e.getMessage());
		}

	}


	/**
	 * 완독
	 * (도서 상태 변경)
	 * ⇒ 진행중(타이머) → 완독(컬렉션)
	 *
	 * @param bookStatusId 유저 책 상태 id
	 */
	public void modifyStatusToDone(Long userId, Long bookStatusId) {
		BookStatusEntity bookStatusEntity = bookStatusRepository.findById(bookStatusId).orElseThrow(() -> new CustomException(ErrorCode.NOTFOUND_RESOURCE));

		// 로그인한 유저의 책이 맞는지 확인
		if (!Objects.equals(bookStatusEntity.getUserId(), userId)) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}

		// 상태 변경
		bookStatusEntity.setStatus(STATUS_DONE);
		bookStatusRepository.save(bookStatusEntity);

		try {
			List<TimerSimpleResponseDto> accumTimeList = timerClient.getAccumTime(List.of(bookStatusId));
			// TODO : 이후 수정하기 - 책 읽은 시간 10페이지 1분 기준으로 통계 반영
			if(!accumTimeList.isEmpty() && accumTimeList.get(0).getAccumTime() > 0){
				bookStatisticRepository.updateReadDatasComplete(bookStatusEntity.getBookId().getId(), accumTimeList.get(0).getAccumTime());
			}
		} catch (FeignException e){
			log.error(e.getMessage());
		}
	}


	/**
	 * 다 읽은 책 컬렉션 조회
	 *
	 * @param userId   유저 id
	 * @param pageable 페이징
	 * @return 컬렉션 리스트
	 */
	public Slice<BookStatusEntity> getCollectionList(Long userId, Pageable pageable) {
		return bookStatusRepository.getByUserIdAndStatusEquals(userId, STATUS_DONE, pageable);
	}


	/**
	 * 다 읽은 책 컬렉션에서 삭제
	 *
	 * @param bookStatusId 책 상태 id
	 */
	@Transactional
	public void deleteCollection(Long userId, Long bookStatusId) {
		BookStatusEntity bookStatusEntity = bookStatusRepository.findById(bookStatusId).orElseThrow(() -> new CustomException(ErrorCode.NOTFOUND_RESOURCE));
		String bookId = bookStatusEntity.getBookId().getId();
		bookStatusRepository.deleteByIdAndUserId(bookStatusId, userId);

		// 책 통계 업데이트
		try {
			List<TimerSimpleResponseDto> accumTimeList = timerClient.getAccumTime(List.of(bookStatusId));
			// TODO : 이후 수정하기 - 책 읽은 시간 10페이지 1분 기준으로 통계 반영
			if(!accumTimeList.isEmpty() && accumTimeList.get(0).getAccumTime() > 0){
				bookStatisticRepository.updateReadDatasDeleteComplete(bookId, accumTimeList.get(0).getAccumTime());
			}
		} catch (FeignException e){
			log.error(e.getMessage());
		}

	}


	/**
	 * userId, bookId로 status 가져오기
	 */
	public BookStatusEntity getStatusByUserIdAndBookId(Long userId, String bookId) {
		BookEntity bookEntity = bookService.getBookReferenceIfExist(bookId);
		BookStatusEntity result = bookStatusRepository.findTopByUserIdAndBookId(userId, bookEntity);
		if (result == null) {
			throw new CustomException(ErrorCode.NOTFOUND_RESOURCE);
		}
		return result;
	}


	/**
	 * id로 status 가져오기
	 */
	public BookStatusEntity getBookStatus(Long bookStatusId) {
		return bookStatusRepository.findById(bookStatusId).orElseThrow(() -> new CustomException(ErrorCode.NOTFOUND_RESOURCE));
	}


	public UserBookInfoDto getUserBookInfo(Long userId, String bookId) {
		// 북마크 여부
		BookEntity bookEntity = bookService.getBookReferenceIfExist(bookId);
		boolean isBookMarked = bookmarkService.isBookmarkedByUserIdAndBookEntity(userId, bookEntity);

		boolean isReading = false;
		boolean isComplete = false;

		// 읽고있는, 다읽은 책 여부 가져오기
		try {
			BookStatusEntity bookStatusEntity = getStatusByUserIdAndBookId(userId, bookId);
			isReading = bookStatusEntity.getStatus().equals(STATUS_IN_PROGRESS);
			isComplete = !isReading;
		} catch (CustomException e) {
			log.debug("book status 없음 (읽고있거나 완독한 기록 없음) ");
		}

		return UserBookInfoDto.builder()
			.isReading(isReading)
			.isComplete(isComplete)
			.isBookMarked(isBookMarked)
			.build();
	}

}
