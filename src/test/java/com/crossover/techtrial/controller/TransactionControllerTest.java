package com.crossover.techtrial.controller;

import static com.crossover.techtrial.model.MembershipStatus.ACTIVE;
import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import java.time.LocalDateTime;

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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.crossover.techtrial.model.Book;
import com.crossover.techtrial.model.Member;
import com.crossover.techtrial.model.Transaction;
import com.crossover.techtrial.repositories.BookRepository;
import com.crossover.techtrial.repositories.MemberRepository;
import com.crossover.techtrial.repositories.TransactionRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class TransactionControllerTest {

  MockMvc mockMvc;
	
  private static final String API_TRANSACTION = "/api/transaction";
  
  @Mock
  private TransactionController transactionController;

  @Autowired
  private TestRestTemplate template;
  
  @Autowired
  private TransactionRepository transactionRepository;

  @Autowired
  private BookRepository bookRepository;

  @Autowired
  private MemberRepository memberRepository;
  
  @Before
  public void setup() throws Exception {
      mockMvc = MockMvcBuilders.standaloneSetup(transactionController).build();
  }

  @After
  public void tearDown() {
	  transactionRepository.deleteAll();
	  bookRepository.deleteAll();
	  memberRepository.deleteAll();
  }

  private final HttpEntity<Object> getHttpEntity(Object body) {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      return new HttpEntity<Object>(body, headers);
  }

  @Test
  public void testTransactionRegistrationSuccessful() throws Exception {
	  // arrange
	  final Member member = saveMember("Gerald F.", "gdf@gmail.com");
	  final Book book = saveBook("Head First Java");
      final HttpEntity<Object> transaction = getHttpEntity(
              "{\"bookId\": " + book.getId() + ", \"memberId\": " + member.getId() + " }");

      // act
      final ResponseEntity<Transaction> response = template.postForEntity(
              API_TRANSACTION, transaction, Transaction.class);

      // assert
      Assert.assertEquals(book.getTitle(), response.getBody().getBook().getTitle());
      Assert.assertEquals(member.getName(), response.getBody().getMember().getName());
      Assert.assertEquals(OK.value(), response.getStatusCode().value());
  }

  @Test
  public void testBookIssuance_BookNotFound() throws Exception {
	  // arrange
	  final Member member = saveMember("Gerald F.", "gdf@gmail.com");
      final HttpEntity<Object> transaction = getHttpEntity(
              "{\"bookId\": 1000000, \"memberId\": " + member.getId() + " }");
      
      // act
      final ResponseEntity<Transaction> response = template.postForEntity(
              API_TRANSACTION, transaction, Transaction.class);
      
      //assert
      Assert.assertEquals(NOT_FOUND.value(), response.getStatusCode().value());
  }
  
  @Test
  public void testBookIssuance_BookAlreadyIssued() throws Exception {
	  // arrange
	  final Member member = saveMember("Gerald F.", "gdf@gmail.com");
	  final Book book = saveBook("Head First Java");
      final HttpEntity<Object> transaction = getHttpEntity(
              "{\"bookId\": " + book.getId() + ", \"memberId\": " + member.getId() + " }");

      // act
      final ResponseEntity<Transaction> response1 = template.postForEntity(
    		  API_TRANSACTION, transaction, Transaction.class);
      final ResponseEntity<Transaction> response2 = template.postForEntity(
    		  API_TRANSACTION, transaction, Transaction.class);
      
      // assert
      Assert.assertEquals(book.getTitle(), response1.getBody().getBook().getTitle());
      Assert.assertEquals(member.getName(), response1.getBody().getMember().getName());
      Assert.assertEquals(OK.value(), response1.getStatusCode().value());
      Assert.assertEquals(FORBIDDEN.value(), response2.getStatusCode().value());
  }
  
  @Test
  public void testBookIssuance_has5IssuedBooks() {
	  // Arrange
	  final Member member = saveMember("Gerald F.", "gdf@gmail.com");
    
	  for (int i = 0; i < 6; i++) {
    	final Book book = saveBook("Head First Java Volume " + i + "");
      
    	final HttpEntity<Object> transaction = getHttpEntity(
              "{\"bookId\": " + book.getId() + ", \"memberId\": " + member.getId() + " }");
      
    	// Act
    	final ResponseEntity<Transaction> response = template
    		.postForEntity(API_TRANSACTION, transaction, Transaction.class);

    	// Assert
    	if (i < 5) {
    		assertEquals(OK.value(), response.getStatusCode().value());
    	} else {
    		assertEquals(FORBIDDEN.value(), response.getStatusCode().value());
    	}
	  }
  }
  
  @Test
  public void testReturnBook_Successful() throws Exception {
	  // arrange
	  final Member member = saveMember("Gerald F.", "gdf4@gmail.com");
	  final Book book = saveBook("Head First Java");
	  final HttpEntity<Object> transaction = getHttpEntity(
              "{\"bookId\": " + book.getId() + ", \"memberId\": " + member.getId() + " }");

	  // act
	  final ResponseEntity<Transaction> issuanceResponse = template.postForEntity(
    		  API_TRANSACTION, transaction, Transaction.class);
	  final Long transactionId = requireNonNull(issuanceResponse.getBody()).getId();
	  final String uri = API_TRANSACTION + "/" + transactionId + "/return?_method=patch";
	  final ResponseEntity<Transaction> returnResponse = template.postForEntity(
			  uri, transaction, Transaction.class);

	  // assert
      Assert.assertEquals(book.getTitle(), issuanceResponse.getBody().getBook().getTitle());
      Assert.assertEquals(member.getName(), issuanceResponse.getBody().getMember().getName());
      Assert.assertNotEquals(null, issuanceResponse.getBody().getDateOfIssue());
      Assert.assertEquals(OK.value(), issuanceResponse.getStatusCode().value());
	  Assert.assertEquals(OK.value(), returnResponse.getStatusCode().value());
  }

  @Test
  public void testReturnBook_SameTransactionId()  {
	  // arrange
	  final Book book = saveBook("Learning Reactive Programming With Java 8");
	  final Member member = saveMember("Gerald F", "gdf@gmail.com");
      final HttpEntity<Object> transaction = getHttpEntity(
              "{\"bookId\": " + book.getId() + ", \"memberId\": " + member.getId() + " }");

      // act
   	  final ResponseEntity<Transaction> issuanceResponse = template.postForEntity(
       		  API_TRANSACTION, transaction, Transaction.class);
   	  final Long transactionId = requireNonNull(issuanceResponse.getBody()).getId();
   	  final String uri = API_TRANSACTION + "/" + transactionId + "/return?_method=patch";
   	  final ResponseEntity<Transaction> returnResponse1 = template.postForEntity(
   			  uri, transaction, Transaction.class);
   	  final ResponseEntity<Transaction> returnResponse2 = template.postForEntity(
 			  uri, transaction, Transaction.class);

	  // assert
	  Assert.assertEquals(OK.value(), returnResponse1.getStatusCode().value());
	  Assert.assertEquals(FORBIDDEN.value(), returnResponse2.getStatusCode().value());
  }
  
  public static final HttpEntity<Object> getRequestHeaders() {
	  final HttpHeaders headers = new HttpHeaders();
	  final MediaType mediaType = new MediaType("application", "MediaType.APPLICATION_JSON");
	  headers.setContentType(mediaType);

	  return new HttpEntity<>(headers);
  }
  
  private Book saveBook(String title) {
	  Book book = new Book();
	  book.setTitle(title);
	  return bookRepository.save(book);
  }
  
  private Member saveMember(String name, String email) {
	  Member member = new Member();
	  member.setName(name);
	  member.setEmail(email);
	  member.setMembershipStatus(ACTIVE);
	  member.setMembershipStartDate(LocalDateTime.now());
	  return memberRepository.save(member);
  }
}