package com.dokki.book.dto;


import com.dokki.book.dto.response.StartEndDateResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBookInfoDto {

	private String bookId;
	private boolean isBookMarked;
	private boolean isReading;
	private boolean isComplete;
	private StartEndDateResponseDto startEndDate;
	private int accumTime;

}
