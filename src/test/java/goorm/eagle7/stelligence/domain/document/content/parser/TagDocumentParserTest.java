package goorm.eagle7.stelligence.domain.document.content.parser;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.owasp.html.PolicyFactory;

import goorm.eagle7.stelligence.domain.document.content.dto.SectionRequest;
import goorm.eagle7.stelligence.domain.section.model.Heading;

@ExtendWith(MockitoExtension.class)
class TagDocumentParserTest {

	@Mock
	PolicyFactory policyFactory;

	@InjectMocks
	TagDocumentParser tagDocumentParser;

	@Test
	@DisplayName("정상 요청")
	void parse() {
		//given
		String rawContent =
			"<h1>title</h1>"
				+ "<p>content</p>"
				+ "<h2>title2</h2>"
				+ "<p>content2 line 1</p>"
				+ "<p>content2 line 2</p>"
				+ "<h3>title3</h3>"
				+ "<p>content3</p>";

		//when
		Mockito.when(policyFactory.sanitize(rawContent)).thenReturn(rawContent);
		List<SectionRequest> result = tagDocumentParser.parse(rawContent);

		//then
		assertThat(result).hasSize(3);

		assertThat(result.get(0).getHeading()).isEqualTo(Heading.H1);
		assertThat(result.get(0).getTitle()).isEqualTo("title");
		assertThat(result.get(0).getContent()).isEqualTo("<p>content</p>");

		assertThat(result.get(1).getHeading()).isEqualTo(Heading.H2);
		assertThat(result.get(1).getTitle()).isEqualTo("title2");
		assertThat(result.get(1).getContent()).isEqualTo("<p>content2 line 1</p><p>content2 line 2</p>");

		assertThat(result.get(2).getHeading()).isEqualTo(Heading.H3);
		assertThat(result.get(2).getTitle()).isEqualTo("title3");
		assertThat(result.get(2).getContent()).isEqualTo("<p>content3</p>");
	}

	@Test
	@DisplayName("시작 부분에 heading tag가 없는 경우")
	void noHeadingTagFirst() {
		//given
		String rawContent =
			"title"
				+ "<p>content</p>"
				+ "<h2>title2</h2>"
				+ "<p>content2 line 1</p>"
				+ "<p>content2 line 2</p>"
				+ "<h3>title3</h3>"
				+ "<p>content3</p>";

		//when
		Mockito.when(policyFactory.sanitize(rawContent)).thenReturn(rawContent);
		List<SectionRequest> result = tagDocumentParser.parse(rawContent);

		//then
		assertThat(result).hasSize(2);

		assertThat(result.get(0).getHeading()).isEqualTo(Heading.H2);
		assertThat(result.get(0).getTitle()).isEqualTo("title2");
		assertThat(result.get(0).getContent()).isEqualTo("<p>content2 line 1</p><p>content2 line 2</p>");

		assertThat(result.get(1).getHeading()).isEqualTo(Heading.H3);
		assertThat(result.get(1).getTitle()).isEqualTo("title3");
		assertThat(result.get(1).getContent()).isEqualTo("<p>content3</p>");
	}

	@Test
	@DisplayName("일부 content가 없는 경우")
	void noContent() {
		//given
		String rawContent =
			"<h1>title</h1>"
				+ "<h2>title2</h2>"
				+ "<p>content2 line 1</p>"
				+ "<p>content2 line 2</p>"
				+ "<h3>title3</h3>"
				+ "<p>content3</p>";

		//when
		Mockito.when(policyFactory.sanitize(rawContent)).thenReturn(rawContent);
		List<SectionRequest> result = tagDocumentParser.parse(rawContent);

		//then
		assertThat(result).hasSize(3);

		assertThat(result.get(0).getHeading()).isEqualTo(Heading.H1);
		assertThat(result.get(0).getTitle()).isEqualTo("title");
		assertThat(result.get(0).getContent()).isEmpty();

		assertThat(result.get(1).getHeading()).isEqualTo(Heading.H2);
		assertThat(result.get(1).getTitle()).isEqualTo("title2");
		assertThat(result.get(1).getContent()).isEqualTo("<p>content2 line 1</p><p>content2 line 2</p>");

		assertThat(result.get(2).getHeading()).isEqualTo(Heading.H3);
		assertThat(result.get(2).getTitle()).isEqualTo("title3");
		assertThat(result.get(2).getContent()).isEqualTo("<p>content3</p>");
	}
}