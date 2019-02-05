package com.crossover.techtrial.controller;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

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
import com.crossover.techtrial.repositories.BookRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class BookControllerTest {

  MockMvc mockMvc;
	
  private static final String API_BOOK = "/api/book";
  
  @Mock
  private BookController bookController;

  @Autowired
  private TestRestTemplate template;

  @Autowired
  private BookRepository bookRepository;

  @Before
  public void setup() throws Exception {
      mockMvc = MockMvcBuilders.standaloneSetup(bookController).build();
  }

  @Test
  public void testBookRegistrationSuccessful() throws Exception {
      final HttpEntity<Object> book = getHttpEntity(
              "{\"title\": \"Head First Java\" }");

      final ResponseEntity<Book> response = template.postForEntity(
              API_BOOK, book, Book.class);

      Assert.assertEquals("Head First Java", response.getBody().getTitle());
      Assert.assertEquals(OK.value(), response.getStatusCode().value());

      //cleanup the book
      bookRepository.deleteById(response.getBody().getId());
  }
  
  @After
  public void tearDown() {
	  bookRepository.deleteAll();
  }
  
  private final HttpEntity<Object> getHttpEntity(Object body) {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      return new HttpEntity<Object>(body, headers);
  }
  
  @Test
  public void testFindById_Successful() throws Exception {
	  // arrange
      final HttpEntity<Object> book = getHttpEntity(
              "{\"title\": \"Head First Java\" }");
      // act
      final ResponseEntity<Book> response = template.postForEntity(
              API_BOOK, book, Book.class);

      // assert
      Assert.assertEquals("Head First Java", response.getBody().getTitle());
      Assert.assertEquals(OK.value(), response.getStatusCode().value());

      final Book m = template.getForObject(API_BOOK + "/" + response.getBody().getId(), Book.class);

      Assert.assertEquals("Head First Java", m.getTitle());
  }

  @Test
  public void testFindById_NotFound() {
	  // act
	  final ResponseEntity<Book> getBookByIdResponse = template
			  .getForEntity(API_BOOK + "/100000", Book.class);

	  // assert
	  Assert.assertEquals(NOT_FOUND.value(), getBookByIdResponse.getStatusCode().value());
  }

  @Test
  public void testFindAllBooks_Successful() throws Exception {
	  // arrange
      final HttpEntity<Object> book = getHttpEntity(
              "{\"title\": \"Head First Java\" }");
      
      // act
      final ResponseEntity<Book> response = template.postForEntity(
              API_BOOK, book, Book.class);

      // assert
      Assert.assertEquals("Head First Java", response.getBody().getTitle());
      Assert.assertEquals(OK.value(), response.getStatusCode().value());

      final ResponseEntity<Book[]> responseEntity = template.getForEntity(API_BOOK, Book[].class);

      Assert.assertTrue((responseEntity.getBody().length > 0));
  }
}
