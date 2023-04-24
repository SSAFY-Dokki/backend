package com.dokki.book.dto.response;


import com.dokki.book.entity.BookEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;


@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookResponseDto {

	private String bookId;
	private String bookTitle;
	private String bookAuthor;
	private String bookCoverPath;
	private String bookPublishYear;


	public static BookResponseDto fromEntity(BookEntity bookEntity) {
		// TODO : 채우기
		return new BookResponseDto();
	}


	public static Page<BookResponseDto> fromEntityPage(Page<BookEntity> bookEntityPage) {
		return bookEntityPage.map(BookResponseDto::fromEntity);
	}

}
