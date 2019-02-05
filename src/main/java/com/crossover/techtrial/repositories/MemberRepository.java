/**
 * 
 */
package com.crossover.techtrial.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;

import com.crossover.techtrial.dto.TopMemberDTO;
import com.crossover.techtrial.model.Member;

/**
 * Person repository for basic operations on Person entity.
 * @author crossover
 */
@RestResource(exported=false)
public interface MemberRepository extends PagingAndSortingRepository<Member, Long> {
  Optional<Member> findById(Long id);
  List<Member> findAll();
  
  /**
   * @return top 5 members who completed the maximum number of transactions(issued/returned books) within
   * the given duration. Completed transaction means that date of issuance and date of return are within
   * the search range. API should return member name, a number of books issued/returned in this duration.
  */
  
  @Query("SELECT "
  		+ "	new com.crossover.techtrial.dto.TopMemberDTO(t.member.id, t.member.name, t.member.email, COUNT(t.member.id)) "
      + "FROM Transaction t "
      + "WHERE "
      + "	t.dateOfIssue >= :startTime AND t.dateOfReturn <= :endTime "
      + "GROUP BY t.member.id, t.member.name, t.member.email "
      + "ORDER BY COUNT(t.member.id) DESC")
  List<TopMemberDTO> findTop5Members(@Param("startTime") LocalDateTime startTime,
      @Param("endTime") LocalDateTime endTime, Pageable pageable);
}
