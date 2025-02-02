package goorm.eagle7.stelligence.domain.document.graph;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.transaction.annotation.Transactional;

import goorm.eagle7.stelligence.domain.document.graph.dto.DocumentNodeResponse;
import goorm.eagle7.stelligence.domain.document.graph.model.DocumentNode;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Transactional
@Slf4j
class DocumentNodeRepositoryTest {

	@Autowired
	private DocumentNodeRepository documentNodeRepository;
	@Autowired
	Neo4jClient neo4jClient;

	/**
	 * 현재 neo4j 상태와 관계없이 테스트 코드가 잘 동작하도록 noe4j를 초기화합니다.
	 * 테스트가 끝난 이후 롤백되면서 기존에 있던 데이터에는 영향을 주지 않습니다.
	 */
	@BeforeEach
	void setupClearNeo4j() {
		String clearQuery = "match (n) detach delete n;";
		neo4jClient.query(clearQuery).run();
	}

	@Test
	@DisplayName("문서 노드 단일 저장 테스트")
	void saveDocumentNode() {

		final long documentId = 6L;
		final String title = "제목1";

		DocumentNode documentNode = new DocumentNode(documentId, title);
		documentNodeRepository.save(documentNode);
		log.info("documentNode.getDocumentId = {}", documentNode.getDocumentId());
		log.info("documentNode.getTitle = {}", documentNode.getTitle());

		DocumentNode findDocumentNode = documentNodeRepository.findById(documentId).orElseThrow();
		assertThat(findDocumentNode.getDocumentId()).isEqualTo(documentId);
	}

	@Test
	@DisplayName("문서 노드 단일 조회 테스트")
	void findDocumentNode() {

		final long documentId = 1L;
		final String title = "제목1";

		final long wrongDocumentId = 2L;
		final String wrongTitle = "다른 노드 제목2";

		DocumentNode documentNode = new DocumentNode(documentId, title);
		documentNodeRepository.save(documentNode);
		DocumentNode wrongDocumentNode = new DocumentNode(wrongDocumentId, wrongTitle);
		documentNodeRepository.save(wrongDocumentNode);

		DocumentNode findNode = documentNodeRepository.findById(documentId).orElseThrow();

		assertThat(findNode.getDocumentId()).isEqualTo(documentNode.getDocumentId());
		assertThat(findNode.getDocumentId()).isNotEqualTo(wrongDocumentNode.getDocumentId());
	}

	@Test
	@DisplayName("문서 노드 조회 실패 테스트")
	void findDocumentNodeFail() {

		final long nonExistDocumentId = 1L;

		Optional<DocumentNode> findNodeOptional = documentNodeRepository.findById(nonExistDocumentId);

		assertThat(findNodeOptional).isEmpty();
	}

	@Test
	@DisplayName("문서 노드 여러 개 저장 테스트")
	void saveDocumentNodes() {

		final long parentDocumentId = 1L;
		final String parentTitle = "제목1";

		final long childDocumentId = 2L;
		final String childTitle = "제목2";

		// 기존에 있었던 DocumentNode인 parentNode
		DocumentNode parentNode = new DocumentNode(parentDocumentId, parentTitle);
		documentNodeRepository.save(parentNode);
		log.info("parentNode.getDocumentId = {}", parentNode.getDocumentId());
		log.info("parentNode.getTitle = {}", parentNode.getTitle());

		// 새로운 DocumentNode인 childNode
		DocumentNode childNode = new DocumentNode(childDocumentId, childTitle, parentNode);
		documentNodeRepository.save(childNode);
		log.info("childNode.getDocumentId = {}", childNode.getDocumentId());
		log.info("childNode.getTitle = {}", childNode.getTitle());

		// 저장된 자식 노드 조회
		DocumentNode findDocumentNode = documentNodeRepository.findById(childDocumentId).orElseThrow();
		assertThat(findDocumentNode.getDocumentId()).isEqualTo(childDocumentId);
		assertThat(findDocumentNode.getTitle()).isEqualTo(childTitle);
		assertThat(findDocumentNode.getGroup()).isEqualTo(parentNode.getGroup());
		assertThat(findDocumentNode.getParentDocumentNode().getDocumentId()).isEqualTo(parentDocumentId);
	}

	@Test
	@DisplayName("문서 노드 3 개 이상 저장 테스트")
	void saveThreeDocumentNodes() {

		final long parentDocumentId = 1L;
		final String parentTitle = "제목1";

		final long childDocumentId = 2L;
		final String childTitle = "제목2";

		final long grandChildDocumentId = 3L;
		final String grandChildTitle = "제목3";

		// 기존에 있었던 DocumentNode인 parentNode
		DocumentNode parentNode = new DocumentNode(parentDocumentId, parentTitle);
		documentNodeRepository.save(parentNode);
		log.info("parentNode.getDocumentId = {}", parentNode.getDocumentId());
		log.info("parentNode.getTitle = {}", parentNode.getTitle());

		// 기존에 있었던 DocumentNode인 childNode
		DocumentNode childNode = new DocumentNode(childDocumentId, childTitle, parentNode);
		documentNodeRepository.save(childNode);
		log.info("childNode.getDocumentId = {}", childNode.getDocumentId());
		log.info("childNode.getTitle = {}", childNode.getTitle());

		// 새로운 DocumentNode인 grandChildNode 저장
		log.info("단일노드 조회 시작");
		DocumentNode findChildNode = documentNodeRepository.findSingleNodeByDocumentId(childDocumentId).get();
		log.info("단일노드 조회 끝");
		DocumentNode grandChildNode = new DocumentNode(grandChildDocumentId, grandChildTitle, findChildNode);
		documentNodeRepository.save(grandChildNode);
		log.info("grandChildNode.getDocumentId = {}", grandChildNode.getDocumentId());
		log.info("grandChildNode.getTitle = {}", grandChildNode.getTitle());
		log.info("grandChildNode.getGroup = {}", grandChildNode.getGroup());

		// 저장된 손자 노드 조회
		DocumentNode findDocumentNode = documentNodeRepository.findById(grandChildDocumentId).orElseThrow();
		assertThat(findDocumentNode.getDocumentId()).isEqualTo(grandChildDocumentId);
		assertThat(findDocumentNode.getTitle()).isEqualTo(grandChildTitle);
		assertThat(findDocumentNode.getGroup()).isEqualTo(parentNode.getGroup());
	}

	@Test
	@DisplayName("특정 제목으로 검색하면 해당 제목이 포함된 경우에만 반환")
	void findNodeByTitle() {

		//given
		final String searchTitle = "title1";
		final int limit = 5;

		String[] queries = queriesThatMakesThreeNodesWithDepthFour();

		for (String queryString : queries) {
			neo4jClient.query(queryString).run();
		}

		//when
		List<DocumentNodeResponse> findNodes = documentNodeRepository.findNodeByTitle(searchTitle, limit);

		//then
		log.info("findNodes: {}", findNodes);
		List<String> titleList = findNodes.stream().map(DocumentNodeResponse::getTitle).toList();
		assertThat(titleList)
			.isNotEmpty()
			.allMatch(s -> s.contains(searchTitle));
	}

	@Test
	@DisplayName("존재하지 않는 제목으로 검색하면 결과가 빈 리스트로 반환")
	void findNoNodeByTitle() {

		//given
		final String searchTitle = "대충검색이될수없는이상한검색어";
		final int limit = 5;

		String[] queries = queriesThatMakesThreeNodesWithDepthFour();

		for (String queryString : queries) {
			neo4jClient.query(queryString).run();
		}

		//when
		List<DocumentNodeResponse> findNodes = documentNodeRepository.findNodeByTitle(searchTitle, limit);

		//then
		log.info("findNodes: {}", findNodes);
		List<String> titleList = findNodes.stream().map(DocumentNodeResponse::getTitle).toList();
		assertThat(titleList)
			.isEmpty();
	}

	@Test
	@DisplayName("한국어 제목으로도 검색이 잘 됨")
	void findKoreanNodeByTitle() {

		//given
		final String searchTitle = "제목";
		final int limit = 5;

		String[] queries = queriesThatMakesNodeInKorean();

		for (String queryString : queries) {
			neo4jClient.query(queryString).run();
		}

		//when
		List<DocumentNodeResponse> findNodes = documentNodeRepository.findNodeByTitle(searchTitle, limit);

		//then
		log.info("findNodes: {}", findNodes);
		List<String> titleList = findNodes.stream().map(DocumentNodeResponse::getTitle).toList();
		assertThat(titleList)
			.isNotEmpty()
			.allMatch(s -> s.contains(searchTitle));
	}

	@Test
	@DisplayName("documentId의 리스트로 문서 노드들 검색")
	void findNodeByDocumentId() {
		//given
		final List<Long> searchDocumentIdList = new ArrayList<>();
		searchDocumentIdList.add(1L);
		searchDocumentIdList.add(2L);
		searchDocumentIdList.add(3L);

		String[] queries = queriesThatMakesThreeNodesWithDepthFour();

		for (String queryString : queries) {
			neo4jClient.query(queryString).run();
		}

		//when
		List<DocumentNodeResponse> findNodes = documentNodeRepository.findNodeByDocumentId(searchDocumentIdList);

		//then
		log.info("findNodes: {}", findNodes);
		List<Long> findDocumentIdList = findNodes.stream().map(DocumentNodeResponse::getDocumentId).toList();
		assertThat(findDocumentIdList)
			.isNotEmpty()
			.hasSize(3)
			.containsAll(searchDocumentIdList);
	}

	@Test
	@DisplayName("루트 노드 여부 테스트")
	void isRootNode() {
		//given
		final Long rootNodeId = 1L;
		final Long nonrootNodeId = 11L;
		final Long nonexistNodeId = 999999L;

		String[] queries = queriesThatMakesThreeNodesWithDepthFour();

		for (String queryString : queries) {
			neo4jClient.query(queryString).run();
		}

		//when
		Optional<Boolean> rootIsRoot = documentNodeRepository.isRootNode(rootNodeId);
		Optional<Boolean> nonrootIsRoot = documentNodeRepository.isRootNode(nonrootNodeId);
		Optional<Boolean> emptuIsRoot = documentNodeRepository.isRootNode(nonexistNodeId);

		//then
		assertThat(rootIsRoot).isNotEmpty().hasValue(true);
		assertThat(nonrootIsRoot).isNotEmpty().hasValue(false);
		assertThat(emptuIsRoot).isEmpty();
	}

	@Test
	@DisplayName("최상위 문서가 아닌 노드 삭제 시, 하위 문서는 상위 문서의 관계를 물려받게 된다.")
	void deleteNonrootNodeByDocumentId() {
		//given
		final Long deleteTargetId = 11L;
		final Long parentIdOfDeleteTargetId = 1L;
		final List<Long> childIdListOfDeleteTarget = List.of(111L, 112L, 113L);

		String[] queries = queriesThatMakesThreeNodesWithDepthFour();

		for (String queryString : queries) {
			neo4jClient.query(queryString).run();
		}

		//when
		documentNodeRepository.deleteNonrootNodeByDocumentId(deleteTargetId);

		//then
		assertThat(documentNodeRepository.findById(deleteTargetId)).isEmpty();
		List<DocumentNode> childNodeList = documentNodeRepository.findAllById(childIdListOfDeleteTarget);
		List<DocumentNode> parentNodeList = childNodeList.stream().map(DocumentNode::getParentDocumentNode).toList();

		assertThat(parentNodeList)
			.isNotEmpty()
			.allMatch(p -> p.getDocumentId().equals(parentIdOfDeleteTargetId));
	}

	@Test
	@DisplayName("최상위 문서 노드 삭제 시에는 하위 문서들이 최상위 문서가 된다.")
	void deleteRootByDocumentId() {
		// given
		final Long deleteTargetId = 1L;
		final List<Long> childIdListOfDeleteTarget = List.of(11L, 12L, 13L);

		String[] queries = queriesThatMakesThreeNodesWithDepthFour();

		for (String queryString : queries) {
			neo4jClient.query(queryString).run();
		}

		//when
		documentNodeRepository.deleteRootNodeByDocumentId(deleteTargetId);

		//then
		assertThat(documentNodeRepository.findById(deleteTargetId)).isEmpty();

		List<DocumentNode> childNodeList = documentNodeRepository.findAllById(childIdListOfDeleteTarget);
		List<Boolean> isRootList = childNodeList.stream()
			.map(DocumentNode::getDocumentId)
			.map(id -> documentNodeRepository.isRootNode(id).get())
			.toList();

		assertThat(isRootList)
			.isNotEmpty()
			.allMatch(b -> b);
	}

	@Test
	@DisplayName("링크 수정 테스트")
	void changeLinkToUpdateParent() {
		// given
		final Long updateTargetId = 11L;
		final List<Long> childIdListOfUpdateTarget = List.of(111L, 112L, 113L);
		final Long newParentNodeId = 3L;

		String[] queries = queriesThatMakesThreeNodesWithDepthFour();

		for (String queryString : queries) {
			neo4jClient.query(queryString).run();
		}

		//when
		documentNodeRepository.changeLinkToUpdateParent(updateTargetId, newParentNodeId);

		//then
		Optional<DocumentNode> targetNodeOptional = documentNodeRepository.findById(updateTargetId);
		assertThat(targetNodeOptional).isPresent();
		DocumentNode updateNode = targetNodeOptional.get();
		assertThat(updateNode.getParentDocumentNode().getDocumentId()).isEqualTo(newParentNodeId);
		assertThat(updateNode.getGroup()).isEqualTo(updateNode.getParentDocumentNode().getGroup());

		List<DocumentNodeResponse> childDocuments = documentNodeRepository.findNodeByDocumentId(
			childIdListOfUpdateTarget);
		assertThat(childDocuments)
			.isNotEmpty()
			.allMatch(n -> n.getGroup().equals(updateNode.getGroup()));
	}

	@Test
	@DisplayName("링크 삭제 테스트")
	void removeLink() {
		// given
		final Long updateTargetId = 11L;
		final List<Long> childIdListOfUpdateTarget = List.of(111L, 112L, 113L);

		String[] queries = queriesThatMakesThreeNodesWithDepthFour();

		for (String queryString : queries) {
			neo4jClient.query(queryString).run();
		}

		//when
		documentNodeRepository.removeLink(updateTargetId);

		//then
		Optional<DocumentNode> targetNodeOptional = documentNodeRepository.findById(updateTargetId);
		assertThat(targetNodeOptional).isPresent();
		DocumentNode targetNode = targetNodeOptional.get();
		assertThat(targetNode.getParentDocumentNode()).isNull();
		assertThat(targetNode.getGroup()).isEqualTo(targetNode.getTitle());

		List<DocumentNodeResponse> childDocuments = documentNodeRepository.findNodeByDocumentId(
			childIdListOfUpdateTarget);
		assertThat(childDocuments)
			.isNotEmpty()
			.allMatch(n -> n.getGroup().equals(targetNode.getGroup()));
	}

	@Test
	@DisplayName("루트 아닌 문서노드 제목 변경 테스트")
	void updateNonrootNodeTitle() {
		// given
		final Long updateTargetId = 11L;
		final String updateTitle = "changedTitle";

		String[] queries = queriesThatMakesThreeNodesWithDepthFour();

		for (String queryString : queries) {
			neo4jClient.query(queryString).run();
		}

		//when
		documentNodeRepository.updateNonrootNodeTitle(updateTargetId, updateTitle);

		//then
		DocumentNode documentNode = documentNodeRepository.findSingleNodeByDocumentId(updateTargetId).get();
		assertThat(documentNode.getTitle()).isEqualTo(updateTitle);
	}

	@Test
	@DisplayName("루트 문서노드 제목 변경 테스트")
	void updateRootNodeTitle() {
		// given
		final Long updateTargetId = 1L;
		final List<Long> childIdListOfUpdateTarget = List.of(11L, 111L, 1111L);
		final String updateTitle = "changedTitle";

		String[] queries = queriesThatMakesThreeNodesWithDepthFour();

		for (String queryString : queries) {
			neo4jClient.query(queryString).run();
		}

		//when
		documentNodeRepository.updateRootNodeTitle(updateTargetId, updateTitle);

		//then
		DocumentNode updatedNode = documentNodeRepository.findSingleNodeByDocumentId(updateTargetId).get();
		List<DocumentNodeResponse> childDocuments = documentNodeRepository.findNodeByDocumentId(
			childIdListOfUpdateTarget);

		assertThat(updatedNode.getTitle()).isEqualTo(updateTitle);
		assertThat(updatedNode.getGroup()).isEqualTo(updateTitle);
		assertThat(childDocuments)
			.isNotEmpty()
			.allMatch(n -> n.getGroup().equals(updatedNode.getGroup()));
	}

	private static String[] queriesThatMakesThreeNodesWithDepthFour() {
		return new String[] {
			"CREATE (:DocumentNode {documentId: 1, level: 2, title: 'title1', group: 'title1'}),"
				+ "       (:DocumentNode {documentId: 2, level: 2, title: 'title2', group: 'title2'}),"
				+ "       (:DocumentNode {documentId: 3, level: 2, title: 'title3', group: 'title3'});",
			"MATCH (p2:DocumentNode {level: 2})"
				+ " WITH p2"
				+ " CREATE (p2)-[:HAS_CHILD]->(:DocumentNode {documentId: p2.documentId * 10 + 1, level: 3, title: 'title' + toString(p2.documentId * 10 + 1), group: p2.group}),"
				+ "       (p2)-[:HAS_CHILD]->(:DocumentNode {documentId: p2.documentId * 10 + 2, level: 3, title: 'title' + toString(p2.documentId * 10 + 2), group: p2.group}),"
				+ "       (p2)-[:HAS_CHILD]->(:DocumentNode {documentId: p2.documentId * 10 + 3, level: 3, title: 'title' + toString(p2.documentId * 10 + 3), group: p2.group});",
			"MATCH (p3:DocumentNode {level: 3})"
				+ " WITH p3"
				+ " CREATE (p3)-[:HAS_CHILD]->(:DocumentNode {documentId: p3.documentId * 10 + 1, level: 4, title: 'title' + toString(p3.documentId * 10 + 1), group: p3.group}),"
				+ "       (p3)-[:HAS_CHILD]->(:DocumentNode {documentId: p3.documentId * 10 + 2, level: 4, title: 'title' + toString(p3.documentId * 10 + 2), group: p3.group}),"
				+ "       (p3)-[:HAS_CHILD]->(:DocumentNode {documentId: p3.documentId * 10 + 3, level: 4, title: 'title' + toString(p3.documentId * 10 + 3), group: p3.group});",
			" MATCH (p4:DocumentNode {level: 4})"
				+ " WITH p4"
				+ " CREATE (p4)-[:HAS_CHILD]->(:DocumentNode {documentId: p4.documentId * 10 + 1, level: 5, title: 'title' + toString(p4.documentId * 10 + 1), group: p4.group}),"
				+ "       (p4)-[:HAS_CHILD]->(:DocumentNode {documentId: p4.documentId * 10 + 2, level: 5, title: 'title' + toString(p4.documentId * 10 + 2), group: p4.group}),"
				+ "       (p4)-[:HAS_CHILD]->(:DocumentNode {documentId: p4.documentId * 10 + 3, level: 5, title: 'title' + toString(p4.documentId * 10 + 3), group: p4.group});",
			"MATCH (n:DocumentNode)"
				+ " SET n.level = NULL;"
		};
	}

	private static String[] queriesThatMakesNodeInKorean() {
		return new String[] {
			"CREATE (:DocumentNode {documentId: 1, title: '제목', group: '제목'}),"
				+ " (:DocumentNode {documentId: 2, title: '제목찾기', group: '제목찾기'}),"
				+ " (:DocumentNode {documentId: 3, title: '제목검색', group: '제목검색'}),"
				+ " (:DocumentNode {documentId: 4, title: '목재', group: '목재'}),"
				+ " (:DocumentNode {documentId: 5, title: '제목과 목차', group: '제목과 목차'}),"
				+ " (:DocumentNode {documentId: 6, title: '제목예시', group: '제목예시'}),"
				+ " (:DocumentNode {documentId: 7, title: '제 목', group: '제 목'}),"
				+ " (:DocumentNode {documentId: 8, title: '제몫', group: '제몫'}),"
				+ " (:DocumentNode {documentId: 9, title: '제모', group: '제모'}),"
				+ " (:DocumentNode {documentId: 10, title: '제뭐임', group: '제뭐임'});"
		};
	}
}