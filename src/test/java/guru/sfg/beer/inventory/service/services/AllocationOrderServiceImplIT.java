package guru.sfg.beer.inventory.service.services;

import guru.sfg.beer.inventory.service.config.JmsConfig;
import guru.sfg.beer.inventory.service.domain.BeerInventory;
import guru.sfg.beer.inventory.service.repositories.BeerInventoryRepository;
import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.model.BeerOrderLineDto;
import guru.sfg.brewery.model.events.AllocateOrderRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.ActiveMQServers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class AllocationOrderServiceImplIT {

  public static final String UPC = "123";
  public static final UUID UUID_NUMBER = UUID.randomUUID();

  @Autowired JmsTemplate jmsTemplate;
  @Autowired
  BeerInventoryRepository repository;
  private static ActiveMQServer embeddedServer;

  @BeforeAll
  public static void startServer() throws Exception {
    // Start the embedded ActiveMQ Artemis server
    Configuration config = new ConfigurationImpl()
            .setPersistenceEnabled(false) // Disable persistence for testing
            .setSecurityEnabled(false) // Disable security for simplicity
            .addAcceptorConfiguration("invm", "vm://0"); // Use in-memory transport

    embeddedServer = ActiveMQServers.newActiveMQServer(config);
    embeddedServer.start();

  }

  @AfterAll
  public static void stopServer() throws Exception {
    // Stop the embedded server after the tests
    if (embeddedServer != null) {
      embeddedServer.stop();
    }
  }

  @Test
  void testMakeAllocated() throws InterruptedException {
    BeerInventory initialInventory = repository.save(BeerInventory.builder().upc(UPC).beerId(UUID_NUMBER).quantityOnHand(100).build());

    AllocateOrderRequest orderRequest =
        AllocateOrderRequest.builder()
            .beerOrderDto(
                BeerOrderDto.builder()
                    .beerOrderLines(
                        List.of(
                            BeerOrderLineDto.builder()
                                .beerId(UUID_NUMBER)
                                .beerName("blabla")
                                .beerStyle("balal")
                                .orderQuantity(1)
                                .upc(UPC)
                                .build()))
                    .build())
            .build();

    jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_QUEUE, orderRequest);

    Thread.sleep(5000);
    Optional<BeerInventory> beerInventory = repository.findById(initialInventory.getId());

    assertThat(beerInventory.isPresent()).isTrue();
    assertThat(beerInventory.get().getQuantityOnHand()).isEqualTo(99);
  }
}
