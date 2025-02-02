package goorm.eagle7.stelligence.domain.document.content.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

import goorm.eagle7.stelligence.domain.document.content.dto.SectionRequest;
import goorm.eagle7.stelligence.domain.section.model.Heading;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * HTML 태그로 들어온 문서를 파싱하여 SectionRequest로 변환하는 클래스입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TagDocumentParser implements DocumentParser {

	private final PolicyFactory policyFactory;

	// Heading 태그를 기반으로 문서를 파싱하기 위한 정규식
	private static final Pattern HEADING_TAG_PATTERN = Pattern.compile(
		"(<h([1-6])>(.*?)</h\\2>)(.*?)(?=<h[1-6]>|$)", Pattern.DOTALL);

	/**
	 * HTML 태그로 들어온 문서를 파싱하여 SectionRequest로 변환합니다.
	 * @param rawContent HTML 태그로 들어온 문서
	 * @return SectionRequest 리스트
	 */
	@Override
	public List<SectionRequest> parse(String rawContent) {
		// 악성 스크립트를 방지하기 위해 HTML를 필터링합니다.
		String sanitizedContent = policyFactory.sanitize(rawContent);

		List<SectionRequest> sectionRequests = new ArrayList<>();
		Matcher matcher = HEADING_TAG_PATTERN.matcher(sanitizedContent);

		while (matcher.find()) {
			Heading heading = Heading.valueOf("H" + matcher.group(2)); // h 태그의 숫자를 Heading enum의 이름으로 사용합니다.
			String title = matcher.group(3); // 타이틀은 h 태그 사이의 내용입니다.
			String content = matcher.group(4).trim(); // 콘텐츠는 다음 h 태그 전까지입니다.

			sectionRequests.add(new SectionRequest(heading, title, content));
		}

		return sectionRequests;
	}
}
