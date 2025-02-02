package goorm.eagle7.stelligence.domain.debate.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import goorm.eagle7.stelligence.config.mockdata.WithMockData;
import goorm.eagle7.stelligence.domain.debate.dto.DebateOrderCondition;
import goorm.eagle7.stelligence.domain.debate.model.Debate;
import goorm.eagle7.stelligence.domain.debate.model.DebateStatus;
import lombok.extern.slf4j.Slf4j;

@DataJpaTest
@WithMockData
@Slf4j
class DebateRepositoryTest {

	@Autowired
	private DebateRepository debateRepository;

	@Test
	@DisplayName("Contribute 페치 조인하여 토론 조회")
	void findByIdWithContribute() {
		Long debateId = 1L;
		Debate findDebate = debateRepository.findByIdWithContribute(debateId).get();

		assertThat(findDebate.getId()).isEqualTo(debateId);

		// 페치 조인 잘 되었는지 테스트
		assertThat(AopUtils.isAopProxy(findDebate.getContribute())).isFalse();
		assertThat(AopUtils.isAopProxy(findDebate.getContribute().getMember())).isFalse();
		assertThat(AopUtils.isAopProxy(findDebate.getContribute().getAmendments().stream().findAny())).isFalse();
	}

	@Test
	@DisplayName("닫힌 토론을 최신순으로 조회")
	void findPageByCloseStatusOrderByLatest() {
		Page<Debate> debatePage = debateRepository.findPageByStatusAndOrderCondition(
			DebateStatus.CLOSED, DebateOrderCondition.LATEST, PageRequest.of(0, 2));
		List<Debate> debates = debatePage.getContent();
		Set<Debate> debateSet = new HashSet<>(debates);

		assertThat(debates)
			.isNotEmpty()
			.hasSize(2)
			.allMatch(d -> d.getStatus().equals(DebateStatus.CLOSED));

		// 중복이 없는지 테스트
		assertThat(debateSet)
			.isNotEmpty()
			.hasSize(2);
	}

	@Test
	@DisplayName("열린 토론을 최근 댓글순으로 조회")
	void findPageByOpenStatusOrderByRecent() {
		Page<Debate> debatePage = debateRepository.findPageByStatusAndOrderCondition(
			DebateStatus.OPEN, DebateOrderCondition.RECENT_COMMENTED, PageRequest.of(0, 2));

		List<Debate> debates = debatePage.getContent();
		Set<Debate> debateSet = new HashSet<>(debates);
		List<Long> debateIdList = debates.stream().map(Debate::getId).toList();

		assertThat(debates)
			.isNotEmpty()
			.hasSize(2)
			.allMatch(d -> d.getStatus().equals(DebateStatus.OPEN));

		// 중복이 없는지 테스트
		assertThat(debateSet)
			.isNotEmpty()
			.hasSize(2);

		// 적절한 순서로 조회되었는지 테스트
		log.info("debateIdList = {}", debateIdList);
		assertThat(debateIdList)
			.isNotEmpty()
			.containsExactly(1L, 3L);
	}

	@Test
	@DisplayName("종료 시간을 기준으로 토론 ID 조회")
	void findOpenDebateIdByEndAt() {

		// when
		List<Long> debateIdList = debateRepository.findOpenDebateIdByEndAt(LocalDateTime.now());

		// then
		List<Debate> debateList = debateRepository.findAllById(debateIdList);
		assertThat(debateList)
			.isNotEmpty()
			.hasSize(4)
			.allMatch(d -> d.getStatus().equals(DebateStatus.OPEN))
			.allMatch(d -> d.getEndAt().isBefore(LocalDateTime.now()));
	}

	@Test
	@DisplayName("토론 종료")
	void closeAllById() {

		// when
		List<Long> debateIdList = debateRepository.findOpenDebateIdByEndAt(LocalDateTime.now());
		debateRepository.closeAllById(debateIdList);

		// then
		List<Debate> debateList = debateRepository.findAllById(debateIdList);
		assertThat(debateList)
			.isNotEmpty()
			.hasSize(4)
			.allMatch(d -> d.getStatus().equals(DebateStatus.CLOSED));
	}

	@Test
	@DisplayName("Document에 대한 특정 상태의 토론이 존재하는지 확인")
	void existsByContributeDocumentIdAndStatus() {

		// when
		boolean res1 = debateRepository.existsByContributeDocumentIdAndStatus(1L, DebateStatus.OPEN);
		boolean res2 = debateRepository.existsByContributeDocumentIdAndStatus(2L, DebateStatus.OPEN);
		boolean res3 = debateRepository.existsByContributeDocumentIdAndStatus(3L, DebateStatus.OPEN);
		boolean res4 = debateRepository.existsByContributeDocumentIdAndStatus(4L, DebateStatus.OPEN);

		// then
		assertThat(res1).isTrue();
		assertThat(res2).isTrue();
		assertThat(res3).isTrue();
		assertThat(res4).isFalse();
	}

	@Test
	@DisplayName("특정 Document의 가장 최근 토론 조회")
	void findLatestDebateByDocument() {
		//given
		
		//when
		Optional<Debate> latestDebateOptional = debateRepository.findLatestDebateByDocumentId(1L);

		//then
		assertThat(latestDebateOptional)
			.isPresent();
		assertThat(latestDebateOptional.get().getId()).isEqualTo(2L);
	}
}