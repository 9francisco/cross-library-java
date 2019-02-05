/**
 * 
 */
package com.crossover.techtrial.controller;

import static com.crossover.techtrial.model.MembershipStatus.ACTIVE;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Objects.requireNonNull;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.crossover.techtrial.dto.TopMemberDTO;
import com.crossover.techtrial.model.Book;
import com.crossover.techtrial.model.Member;
import com.crossover.techtrial.model.Transaction;
import com.crossover.techtrial.repositories.BookRepository;
import com.crossover.techtrial.repositories.MemberRepository;
import com.crossover.techtrial.repositories.TransactionRepository;

/**
 * @author kshah
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class MemberControllerTest {
  
  MockMvc mockMvc;
  
  private static final String API_MEMBER = "/api/member";
  
  @Mock
  private MemberController memberController;
  
  @Autowired
  private TestRestTemplate template;
  
  @Autowired
  private MemberRepository memberRepository;

  @Autowired
  private BookRepository bookRepository;

  @Autowired
  private TransactionRepository transactionRepository;
  
  @Before
  public void setup() throws Exception {
    mockMvc = MockMvcBuilders.standaloneSetup(memberController).build();
  }
  
  @Test
  public void testMemberRegistrationSuccessful() throws Exception {
	  // arrange
	  final HttpEntity<Object> member = getHttpEntity(
			  "{\"name\": \"Gerald F\", \"email\": \"gdf@gmail.com\"," +
			  " \"membershipStatus\": \"ACTIVE\",\"membershipStartDate\":\"2019-02-02T12:12:12\" }");
    
	  // act
	  final ResponseEntity<Member> response = template.postForEntity(
			  API_MEMBER, member, Member.class);
    
	  // assert
	  Assert.assertEquals("Gerald F", response.getBody().getName());
	  Assert.assertEquals(OK.value(), response.getStatusCode().value());
    
	  // cleanup the user
	  memberRepository.deleteById(response.getBody().getId());
  }

  private final HttpEntity<Object> getHttpEntity(Object body) {
	  HttpHeaders headers = new HttpHeaders();
	  headers.setContentType(MediaType.APPLICATION_JSON);
	  return new HttpEntity<Object>(body, headers);
  }
  
  @After
  public void tearDown() {
	  transactionRepository.deleteAll();
	  bookRepository.deleteAll();
	  memberRepository.deleteAll();
  }

  @Test
  public void testDuplicateEmail() throws Exception {
	  // arrange
      final HttpEntity<Object> member1 = getHttpEntity(
    		  "{\"name\": \"Gerald F\", \"email\": \"gdf@gmail.com\"," +
              " \"membershipStatus\": \"ACTIVE\",\"membershipStartDate\":\"2019-02-02T12:12:12\" }");
      final HttpEntity<Object> member2 = getHttpEntity(
              "{\"name\": \"Mylen F\", \"email\": \"gdf@gmail.com\"," +
              " \"membershipStatus\": \"ACTIVE\",\"membershipStartDate\":\"2019-02-02T12:12:12\" }");

      // act
      final ResponseEntity<Member> response1= template.postForEntity(
    		  API_MEMBER, member1, Member.class);
      final ResponseEntity<Member> response2 = template.postForEntity(
    		  API_MEMBER, member2, Member.class);
      
      // assert
      Assert.assertEquals("Gerald F", response1.getBody().getName());
      Assert.assertEquals(OK.value(), response1.getStatusCode().value());
      Assert.assertEquals(BAD_REQUEST.value(), response2.getStatusCode().value());
  }

  @Test
  public void testInvalidEmail() throws Exception {
	  // arrange
	  final HttpEntity<Object> member1 = getHttpEntity(
              "{\"name\": \"member 1\", \"email\": \"InvalidEmail\"," +
              " \"membershipStatus\": \"ACTIVE\",\"membershipStartDate\":\"2019-02-02T12:12:12\" }");
	  final HttpEntity<Object> member2 = getHttpEntity(
              "{\"name\": \"member 2\", \"email\": \"\"," +
              " \"membershipStatus\": \"ACTIVE\",\"membershipStartDate\":\"2019-02-02T12:12:12\" }");
	  
	  // act
	  final ResponseEntity<Member> response1 = template.postForEntity(
			API_MEMBER, member1, Member.class);
	  final ResponseEntity<Member> response2 = template.postForEntity(
        API_MEMBER, member2, Member.class);

	  // assert
	  Assert.assertEquals(BAD_REQUEST.value(), response1.getStatusCode().value());
	  Assert.assertEquals(BAD_REQUEST.value(), response2.getStatusCode().value());
  }
  
  @Test
  public void testNameWithWrongStart() throws Exception {
	  // arrange
      final HttpEntity<Object> member = getHttpEntity(
              "{\"name\": \"9erald F\", \"email\": \"gdf@gmail.com\"," +
              " \"membershipStatus\": \"ACTIVE\",\"membershipStartDate\":\"2019-02-02T12:12:12\" }");
      
      // act
      final ResponseEntity<Member> response = template.postForEntity(
    		  API_MEMBER, member, Member.class);
      
      // assert
      Assert.assertEquals(BAD_REQUEST.value(), response.getStatusCode().value());
  }

  @Test
  public void testTooShortMemberName() throws Exception {
	  // arrange
      final HttpEntity<Object> member = getHttpEntity(
              "{\"name\": \"g\", \"email\": \"gdf@gmail.com\"," +
              " \"membershipStatus\": \"ACTIVE\",\"membershipStartDate\":\"2019-02-02T12:12:12\" }");
      
      // act
      final ResponseEntity<Member> response = template.postForEntity(
    		  API_MEMBER, member, Member.class);
      
      // assert
      Assert.assertEquals(BAD_REQUEST.value(), response.getStatusCode().value());
  }

  @Test
  public void testTooLongMemberName() throws Exception {
	  // arrange
      final HttpEntity<Object> member = getHttpEntity(
              "{\"name\": \"testing too long member name testing too long member name " +
              " testing too long member name testing too long member name " +
              "testing too long member name testing too long member name \", \"email\": \"gdf@gmail.com\"," +
              " \"membershipStatus\": \"ACTIVE\",\"membershipStartDate\":\"2019-02-02T12:12:12\" }");
      
      // act
      final ResponseEntity<Member> response = template.postForEntity(
    		  API_MEMBER, member, Member.class);
      
      // assert
      Assert.assertEquals(BAD_REQUEST.value(), response.getStatusCode().value());
  }

  @Test
  public void testFindById_Successful() throws Exception {
	  // arrange
      final HttpEntity<Object> member = getHttpEntity(
              "{\"name\": \"Marc Gav\", \"email\": \"marcgav@gmail.com\"," +
              " \"membershipStatus\": \"ACTIVE\",\"membershipStartDate\":\"2019-02-02T12:12:12\" }");
      
      // act
      final ResponseEntity<Member> response = template.postForEntity(
    		  API_MEMBER, member, Member.class);
      
      // assert
      Assert.assertEquals("Marc Gav", response.getBody().getName());
      Assert.assertEquals(OK.value(), response.getStatusCode().value());

      final Member m = template.getForObject(API_MEMBER + "/" + response.getBody().getId(), Member.class);
      
      Assert.assertEquals("Marc Gav", m.getName());
  }

  @Test
  public void testFindById_NotFound() {
    // act
    final ResponseEntity<Member> response = template
        .getForEntity(API_MEMBER + "/100000", Member.class);

    // assert
    Assert.assertEquals(NOT_FOUND.value(), response.getStatusCode().value());
  }

  @Test
  public void testFindAllMembers_Successful() throws Exception {
	  // arrange
      final HttpEntity<Object> member = getHttpEntity(
              "{\"name\": \"Marc Gav\", \"email\": \"marcgav@gmail.com\"," +
              " \"membershipStatus\": \"ACTIVE\",\"membershipStartDate\":\"2019-02-02T12:12:12\" }");
      
      // act
      final ResponseEntity<Member> response = template.postForEntity(
    		  API_MEMBER, member, Member.class);
      final ResponseEntity<Member[]> responseEntity = template.getForEntity(API_MEMBER, Member[].class);

      // assert
      Assert.assertEquals("Marc Gav", response.getBody().getName());
      Assert.assertEquals(OK.value(), response.getStatusCode().value());
      Assert.assertTrue((responseEntity.getBody().length > 0));
  }
  
  @Test
  public void testFindTop5Members() {
	  // arrange
	  final LocalDateTime startDate = LocalDateTime.of(2019, 1, 1, 0, 0);
	  final LocalDateTime endDate = startDate.plusDays(30);
	  createTop5MembersData(startDate, endDate);
	  final String url = createUrl(startDate, endDate);
	  final ParameterizedTypeReference<List<TopMemberDTO>> typeRef =
			  new ParameterizedTypeReference<List<TopMemberDTO>>() {};

	  // act
	  final ResponseEntity<List<TopMemberDTO>> response = template
			  .exchange(url, GET, null, typeRef);
    
      // assert
      Assert.assertEquals(OK.value(), response.getStatusCode().value());
      final List<TopMemberDTO> top5Member = requireNonNull(response.getBody());
      Assert.assertEquals(5, top5Member.size());
      
      for (int i = 0; i < top5Member.size(); i++) {
    	  final TopMemberDTO topMemberDto = top5Member.get(i);
	      int bookCount = 1;
	      if (i == 0) {
	    	  bookCount = 4;
	      } else if (i == 1) {
	    	  bookCount = 3;
	      }
	      
	      Assert.assertEquals("Member " + (char)(i+65), topMemberDto.getName());
	      Assert.assertEquals(bookCount, topMemberDto.getBookCount().intValue());
      }
  }

  private void createTop5MembersData(LocalDateTime startDate, LocalDateTime endDate) {
	  final int booksMembersCount = 10;
	  final Book[] books = new Book[booksMembersCount];
	  final Member[] members = new Member[booksMembersCount];

	  for (int i = 0; i < booksMembersCount; i++) {
		  Book book = new Book();
		  book.setTitle("Head First Java Edition  " + i);
		  books[i] = bookRepository.save(book);
      
		  Member member = new Member();
		  member.setName("Member " + (char)(i+65));
		  member.setEmail("member" + i + "@gmail.com");
		  member.setMembershipStartDate(LocalDateTime.now());
      	  member.setMembershipStatus(ACTIVE);
      
      	  members[i] = memberRepository.save(member);
	  }

	  int memberId = 0;
	  for (int i = 0; i < 10; i++) {
		  if (i == 4 || i >= 7) {
			  ++memberId;
		  }

    	  Transaction transaction = new Transaction();
    	  transaction.setBook(books[i]);
    	  transaction.setMember(members[memberId]);
    	  transaction.setDateOfIssue(startDate.plusDays(i));
    	  transaction.setDateOfReturn(endDate.minusDays(i));
    	  transactionRepository.save(transaction);
	  }
  }

  private String createUrl(LocalDateTime startDate, LocalDateTime endDate) {
	  final String fakeHost = "http://localhost";
	  final String url = fakeHost + API_MEMBER + "/top-member";
	  final String startDateStr = startDate.format(ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
	  final String endTimeStr = endDate.format(ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

	  return fromHttpUrl(url)
        .queryParam("startTime", startDateStr)
        .queryParam("endTime", endTimeStr)
        .build()
        .encode()
        .toUriString()
        .replace(fakeHost, "");
  }
}
