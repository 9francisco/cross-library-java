/**
 * 
 */
package com.crossover.techtrial.controller;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
@RestController
public class TransactionController {
  
  @Autowired TransactionRepository transactionRepository;
  
  @Autowired BookRepository bookRepository;
  
  @Autowired MemberRepository memberRepository;
  /*
   * PLEASE DO NOT CHANGE SIGNATURE OR METHOD TYPE OF END POINTS
   * Example Post Request :  { "bookId":1,"memberId":33 }
   */
  @PostMapping(path = "/api/transaction")
  public ResponseEntity<Transaction> issueBookToMember(@RequestBody Map<String, Long> params){
	  Long bookId = params.get("bookId");
	  Long memberId = params.get("memberId");
    
	  final Optional<Book> bookOptional = bookRepository.findById(bookId);
	  if (!bookOptional.isPresent()) {
		  /* Member trying to issue a book which does not exist in our database, 
		   * API should return HTTP Status code 404.
		   */
		  return ResponseEntity.status(404).build();
	  }
    
	  final Optional<Transaction> notReturnedBook = transactionRepository
            .findNotReturnedBook(bookId);
	  if (notReturnedBook.isPresent()) {
		  /* Member is not allowed to issue a book which is already issued to someone 
		   * and should return HTTP Status code 403.
		   */
		  return ResponseEntity.status(403).build();
	  }
        
	  final boolean has5IssuedBooks = transactionRepository.has5IssuedBooks(memberId);
	  if (has5IssuedBooks) {
		  /* API should reject issuance of more than 5 books at a given time. 
       		* If a member already has 5 books issued on his name, and try to issue another 
       		* API should return HTTP Status code 403.
       		*/
		  return ResponseEntity.status(403).build();
	  }
    
	  final Optional<Member> memberOptional = memberRepository.findById(memberId);

	  if (!memberOptional.isPresent()) {
		  return ResponseEntity.status(404).build();
	  }
    
	  Transaction transaction = new Transaction();
	  transaction.setBook(bookRepository.findById(bookId).orElse(null));
	  transaction.setMember(memberRepository.findById(memberId).get());
	  transaction.setDateOfIssue(LocalDateTime.now());    
	  return ResponseEntity.ok().body(transactionRepository.save(transaction));
  }
  
  /*
   * PLEASE DO NOT CHANGE SIGNATURE OR METHOD TYPE OF END POINTS
   */
  @PatchMapping(path= "/api/transaction/{transaction-id}/return")
  public ResponseEntity<Transaction> returnBookTransaction(@PathVariable(name="transaction-id") Long transactionId){  
	  final Optional<Transaction> optionalTransaction = transactionRepository.findById(transactionId);
	  
      if (!optionalTransaction.isPresent()) {
    	  return ResponseEntity.notFound().build();
      }

      Transaction transaction = optionalTransaction.get();
      
      if (transaction.getDateOfReturn() != null) {
		  /* After returning the book and completing the transaction by updating date of return, 
		   * Any subsequent request to return for the same transaction-id should return 
		   * HTTP Status Code 403. Valid value of Date Of Return field means books are returned.
		   */
		  return ResponseEntity.status(403).build();
	  }

	  transaction.setDateOfReturn(LocalDateTime.now());
	  return ResponseEntity.ok().body(transactionRepository.save(transaction));
  }
}
