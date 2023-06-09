package com.dokki.util.common.error;


import lombok.Getter;


@Getter
public enum ErrorCode {
	INVALID_REQUEST(400, "C001", "잘못된 요청입니다."),
	UNKNOWN_ERROR(400, "C002", "알 수 없는 에러"),
	NOTFOUND_RESOURCE(404, "C003", "해당 자원이 존재하지 않습니다."),
	DUPLICATE_RESOURCE(409, "C004", "이미 존재하는 데이터입니다."),
	UNKNOWN_GATEWAY_ERROR(400, "C005", "Gateway에서 발생한 에러"),

	UNAUTHORIZED(400, "U001", "로그인이 필요합니다."),
	WRONG_TYPE_TOKEN(400, "U002", "잘못된 타입의 토큰입니다."),
	EXPIRED_TOKEN(401, "U003", "만료된 토큰입니다."),
	UNSUPPORTED_TOKEN(401, "U004", "지원하지않는 토큰입니다."),
	ACCESS_DENIED(401, "U005", "접근이 거부된 토큰입니다."),
	WRONG_TOKEN(401, "U006", "잘못된 토큰입니다."),
	LOGOUT_TOKEN(401, "U007", "로그아웃된 토큰입니다."),
	PLZ_RELOGIN(401, "U008", "다시 로그인 해주세요."),

	// Book
	UNABLE_SEARCH(500, "B001", "검색 API를 사용할 수 없습니다."),

	// Review
	AI_IMAGE_COUNT_LIMIT_REACHED(400, "R001", "이미지 생성 횟수 제한에 도달하였습니다."),
	JSON_MAPPING_ERROR(500, "R002", "API의 호출 결과를 Json Mapping 하는 중 에러가 발생하였습니다."),
	AI_API_TOO_MANY_REQUESTS(429, "R003", "연속적으로 너무 많은 API를 호출하였습니다. 다시 시도해주세요."),
	AI_API_CLIENT_ERROR(500, "R004", "API 호출하는 중 에러가 발생하였습니다. 다시 시도해주세요."),
	// File
	FILE_IS_NOT_IMAGE(400, "F001", "업로드 파일의 형식이 이미지 파일 확장자가 아닙니다."),
	FILE_UPLOAD_FAIL(500, "F002", "파일 업로드를 실패하였습니다."),
	FILE_IS_EMPTY(400, "F003", "파일 내용이 비어있습니다.");

	private final int status;
	private final String code;
	private final String description;


	ErrorCode(int status, String code, String description) {
		this.status = status;
		this.code = code;
		this.description = description;
	}
}
