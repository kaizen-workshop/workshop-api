package br.com.weg.workshop.post;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.weg.workshop.TestcontainersConfiguration;
import br.com.weg.workshop.post.domain.Post;
import br.com.weg.workshop.post.repository.PostRepository;
import br.com.weg.workshop.user.domain.*;
import br.com.weg.workshop.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class PostFeedQueryIntegrationTest {

    @Autowired PostRepository posts;
    @Autowired UserRepository users;

    @Test
    void feedIncludesPublishedPostsWithoutLinkedWorkshop() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UserEntity author = users.save(UserEntity.create("Author", "author." + suffix, suffix + "@example.com",
                null, null, null, "hash", Role.ARWEG));
        Post announcement = Post.create("Announcement " + suffix, "Content", null, null, null, false, author);
        announcement.publish();
        posts.saveAndFlush(announcement);

        var feed = posts.findFeed(UUID.randomUUID(), null, PageRequest.of(0, 500));

        assertThat(feed.getContent()).extracting(Post::getId).contains(announcement.getId());
        assertThat(feed.getTotalElements()).isGreaterThanOrEqualTo(feed.getContent().size());
    }
}
