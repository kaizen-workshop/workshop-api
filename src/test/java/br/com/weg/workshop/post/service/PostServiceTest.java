package br.com.weg.workshop.post.service;
import br.com.weg.workshop.audit.service.AuditService;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.weg.workshop.post.domain.*;
import br.com.weg.workshop.post.dto.*;
import br.com.weg.workshop.post.repository.*;
import br.com.weg.workshop.preference.repository.CategoryRepository;
import br.com.weg.workshop.shared.error.ConflictException;
import br.com.weg.workshop.user.domain.*;
import br.com.weg.workshop.user.repository.UserRepository;
import br.com.weg.workshop.workshop.repository.WorkshopRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {
 @Mock PostRepository posts; @Mock PostLikeRepository likes; @Mock PostCommentRepository comments; @Mock UserRepository users; @Mock WorkshopRepository workshops; @Mock CategoryRepository categories; @Mock AuditService audit; @InjectMocks PostService service;
 @Test void createsDraftForAuthorizedAuthor(){UserEntity user=activeUser();when(users.findById(user.getId())).thenReturn(Optional.of(user));when(posts.save(any())).thenAnswer(i->i.getArgument(0));var response=service.create(user.getId(),new PostRequest("Title","Content",null,null,null,true));assertThat(response.status()).isEqualTo("DRAFT");assertThat(response.highlight()).isTrue();}
 @Test void refusesToArchiveDraft(){UserEntity user=activeUser();Post post=Post.create("Title","Content",null,null,null,false,user);when(posts.findById(post.getId())).thenReturn(Optional.of(post));assertThatThrownBy(()->service.archive(user.getId(),false,post.getId())).isInstanceOf(ConflictException.class);}
 @Test void replaysACommentWithTheSameIdempotencyKey(){UserEntity user=activeUser();Post post=publishedPost(user);UUID key=UUID.randomUUID();PostComment comment=PostComment.create(post,user,"Comment",key);when(users.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));when(comments.findByUserIdAndClientOperationId(user.getId(),key)).thenReturn(Optional.of(comment));var response=service.comment(user.getId(),post.getId(),key,new CreateCommentRequest("Comment"));assertThat(response.id()).isEqualTo(comment.getId());verify(comments,never()).save(any());}
 @Test void letsTheAuthorEditAndDeleteAComment(){UserEntity user=activeUser();Post post=publishedPost(user);PostComment comment=PostComment.create(post,user,"Before");when(posts.findById(post.getId())).thenReturn(Optional.of(post));when(comments.findById(comment.getId())).thenReturn(Optional.of(comment));var response=service.editComment(user.getId(),post.getId(),comment.getId(),new CreateCommentRequest("After"));assertThat(response.content()).isEqualTo("After");service.deleteComment(user.getId(),false,post.getId(),comment.getId());verify(comments).delete(comment);}
 private Post publishedPost(UserEntity user){Post post=Post.create("Title","Content",null,null,null,false,user);post.publish();return post;}
 private UserEntity activeUser(){UserEntity u=UserEntity.create("User",UUID.randomUUID().toString(),UUID.randomUUID()+"@example.com",null,null,null,"hash",Role.ARWEG);u.changePassword("hash");return u;}
}
