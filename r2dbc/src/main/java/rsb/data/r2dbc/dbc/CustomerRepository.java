package rsb.data.r2dbc.dbc;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rsb.data.r2dbc.Customer;
import rsb.data.r2dbc.SimpleCustomerRepository;

@Component
@RequiredArgsConstructor
@Log4j2
public class CustomerRepository implements SimpleCustomerRepository {

	private final DatabaseClient databaseClient;

	// <1>
	@Override
	public Flux<Customer> findAll() {
		return databaseClient.select() //
				.from(Customer.class) //
				.fetch() //
				.all();
	}

	// <2>
	@Override
	public Mono<Customer> save(Customer c) {
		return this.databaseClient.insert() //
				.into(Customer.class) //
				.table("customer") //
				.using(c) //
				.map((row, rowMetadata) -> new Customer(row.get("id", Integer.class),
						c.getEmail()))//
				.first();
	}

	@Override
	public Mono<Customer> update(Customer c) {
		return databaseClient.update().table(Customer.class).using(c).fetch()
				.rowsUpdated().filter(countOfUpdates -> countOfUpdates > 0)
				.switchIfEmpty(Mono.error(
						new IllegalArgumentException("couldn't update " + c.toString())))
				.thenMany(findById(c.getId())).single();
	}

	@Override
	public Mono<Customer> findById(Integer id) {
		return this.databaseClient.execute("select * from customer where id = $1") //
				.bind("$1", id) //
				.fetch() //
				.first() //
				.map(map -> new Customer(Integer.class.cast(map.get("id")),
						String.class.cast(map.get("email"))));
	}

	// <3>
	@Override
	public Mono<Void> deleteById(Integer id) {
		return this.databaseClient.execute("DELETE FROM customer where id = $1") //
				.bind("$1", id) //
				.then();
	}

}
