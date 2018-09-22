package rsb.data.mongo;

import com.mongodb.reactivestreams.client.MongoCollection;
import lombok.extern.log4j.Log4j2;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@Log4j2
@DataMongoTest
@RunWith(SpringRunner.class)
public class MongoApplicationTest {

	@Autowired
	private ReactiveMongoTemplate operations;

	@Autowired
	private OrderRepository repository;

	@Before
	public void start() {
		Mono<MongoCollection<Document>> recreateCollection = operations
				.collectionExists(Order.class)
				.flatMap(exists -> exists ? operations.dropCollection(Order.class)
						: Mono.just(exists))
				.then(operations.createCollection(Order.class, CollectionOptions.empty()
						.size(1024 * 1024).maxDocuments(100).capped()));
		StepVerifier.create(recreateCollection).expectNextCount(1).verifyComplete();
	}

	@Test
	public void tail() throws Exception {
		Queue<Order> people = new ConcurrentLinkedQueue<>();
		this.writeAndWait();
		this.writeAndWait();
		this.repository.findByProductId("1").doOnNext(x -> {
			log.info("found: " + x.toString());
			people.add(x);
		}).doOnComplete(() -> log.info("complete"))
				.doOnTerminate(() -> log.info("terminated")).subscribe();
		this.writeAndWait();
		Assertions.assertThat(people).hasSize(3);
	}

	private void writeAndWait() throws InterruptedException {
		StepVerifier.create(repository.save(new Order(UUID.randomUUID().toString(), "1")))
				.expectNextCount(1).verifyComplete();
		Thread.sleep(100);
	}

}