package rsb.data.r2dbc.dbc;

import org.springframework.data.r2dbc.function.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rsb.data.r2dbc.Customer;
import rsb.data.r2dbc.SimpleCustomerRepository;

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@Component
public class CustomerRepository implements SimpleCustomerRepository {

	private final DatabaseClient databaseClient;

	public CustomerRepository(DatabaseClient databaseClient) {
		this.databaseClient = databaseClient;
	}

	@Override
	public Mono<Void> deleteById(Integer id) {
		return this.databaseClient.execute().sql("DELETE FROM customer where id = $1")
				.bind("$1", id).then();
	}

	@Override
	public Mono<Customer> save(Customer c) {
		return this.databaseClient.insert().into(Customer.class).table("customer")
				.using(c).map((row, rowMetadata) -> c).first();
	}

	@Override
	public Flux<Customer> findAll() {
		return databaseClient.select().from(Customer.class).fetch().all();
	}

}
