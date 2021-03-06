/**
 * Copyright 2019 The CloudEvents Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.cloudevents.v03.kafka;

import static io.cloudevents.v03.kafka.Marshallers.binary;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cloudevents.extensions.DistributedTracingExtension;
import io.cloudevents.extensions.ExtensionFormat;
import io.cloudevents.kafka.CloudEventsKafkaProducer;
import io.cloudevents.types.Much;
import io.cloudevents.v03.AttributesImpl;
import io.cloudevents.v03.CloudEventBuilder;
import io.cloudevents.v03.CloudEventImpl;

/**
 * 
 * @author fabiojose
 *
 */
public class KafkaProducerBinaryTest {
	private static final Logger log = 
			LoggerFactory.getLogger(KafkaProducerBinaryTest.class);
		
	private static final Deserializer<String> DESERIALIZER = 
		Serdes.String().deserializer();

	@Test
	public void should_throws_when_producer_is_null() {
		assertThrows(NullPointerException.class, () -> {
			new CloudEventsKafkaProducer<String, AttributesImpl, Much>(null,
					(e) -> null);
		});
	}
	
	@Test
	public void should_be_ok_with_all_required_attributes() throws Exception {
		// setup
		String dataJson = "{\"wow\":\"nice!\"}";
		final Much data = new Much();
		data.setWow("nice!");
		
		CloudEventImpl<Much> ce = 
			CloudEventBuilder.<Much>builder()
				.withId("x10")
				.withSource(URI.create("/source"))
				.withType("event-type")
				.withDatacontenttype("application/json")
				.withSubject("subject")
				.withData(data)
				.build();
		
		final String topic = "binary.t";
		
		MockProducer<String, byte[]> mocked = new MockProducer<String, byte[]>(true,
				new StringSerializer(), new ByteArraySerializer());
					
		try(CloudEventsKafkaProducer<String, AttributesImpl, Much> 
			ceProducer = new CloudEventsKafkaProducer<>(binary(), mocked)){
			// act
			RecordMetadata metadata = 
				ceProducer.send(new ProducerRecord<>(topic, ce)).get();
			
			log.info("Producer metadata {}", metadata);
			
			assertFalse(mocked.history().isEmpty());
			mocked.history().forEach(actual -> {
				// assert
				assertNotNull(actual);
				Header specversion = 
					actual.headers().lastHeader("ce_specversion");
				
				assertNotNull(specversion);
				assertEquals("0.3", DESERIALIZER
						.deserialize(null, specversion.value()));
				
				Header id = 
						actual.headers().lastHeader("ce_id");
				assertNotNull(id);
				assertEquals("x10", DESERIALIZER
						.deserialize(null, id.value()));
				
				Header source =
						actual.headers().lastHeader("ce_source");
				assertNotNull(source);
				assertEquals("/source", DESERIALIZER
						.deserialize(null, source.value()));
				
				Header type = 
						actual.headers().lastHeader("ce_type");
				assertNotNull(source);
				assertEquals("event-type", DESERIALIZER
						.deserialize(null, type.value()));
				
				Header subject = 
						actual.headers().lastHeader("ce_subject");
				assertNotNull(subject);
				assertEquals("subject", DESERIALIZER
						.deserialize(null, subject.value()));
				
				byte[] actualData = actual.value();
				assertNotNull(actualData);
				assertEquals(dataJson, DESERIALIZER
						.deserialize(null, actualData));
			});
		}
	}
	
	@Test
	public void should_be_ok_with_no_data() throws Exception {
		// setup
		CloudEventImpl<Much> ce = 
			CloudEventBuilder.<Much>builder()
				.withId("x10")
				.withSource(URI.create("/source"))
				.withType("event-type")
				.withSchemaurl(URI.create("/schema"))
				.withDatacontenttype("application/json")
				.withSubject("subject")
				.build();
		
		final String topic = "binary.t";
		
		MockProducer<String, byte[]> mocked = new MockProducer<String, byte[]>(true,
				new StringSerializer(), new ByteArraySerializer());
					
		try(CloudEventsKafkaProducer<String, AttributesImpl, Much> 
			ceProducer = new CloudEventsKafkaProducer<>(binary(), mocked)){
			// act
			RecordMetadata metadata = 
				ceProducer.send(new ProducerRecord<>(topic, ce)).get();
			
			log.info("Producer metadata {}", metadata);
			
			assertFalse(mocked.history().isEmpty());
			mocked.history().forEach(actual -> {
				// assert
				assertNotNull(actual);
				Header specversion = 
					actual.headers().lastHeader("ce_specversion");
				
				assertNotNull(specversion);
				assertEquals("0.3", DESERIALIZER
						.deserialize(null, specversion.value()));
				
				Header id = 
						actual.headers().lastHeader("ce_id");
				assertNotNull(id);
				assertEquals("x10", DESERIALIZER
						.deserialize(null, id.value()));
				
				Header source =
						actual.headers().lastHeader("ce_source");
				assertNotNull(source);
				assertEquals("/source", DESERIALIZER
						.deserialize(null, source.value()));
				
				Header type = 
						actual.headers().lastHeader("ce_type");
				assertNotNull(source);
				assertEquals("event-type", DESERIALIZER
						.deserialize(null, type.value()));
				
				Header schemaurl = 
						actual.headers().lastHeader("ce_schemaurl");
				assertNotNull(source);
				assertEquals("/schema", DESERIALIZER
						.deserialize(null, schemaurl.value()));
				
				Header subject = 
						actual.headers().lastHeader("ce_subject");
				assertNotNull(subject);
				assertEquals("subject", DESERIALIZER
						.deserialize(null, subject.value()));
			});
		}
	}
	
	@Test
	public void should_tracing_extension_ok() throws Exception {
		// setup
		final DistributedTracingExtension dt = new DistributedTracingExtension();
		dt.setTraceparent("0");
		dt.setTracestate("congo=4");
		
		final ExtensionFormat tracing = new DistributedTracingExtension.Format(dt);
		
		final Much data = new Much();
		data.setWow("nice!");
		
		CloudEventImpl<Much> ce = 
			CloudEventBuilder.<Much>builder()
				.withId("x10")
				.withSource(URI.create("/source"))
				.withType("event-type")
				.withSchemaurl(URI.create("/schema"))
				.withDatacontenttype("application/json")
				.withSubject("subject")
				.withData(data)
				.withExtension(tracing)
				.build();
		
		final String topic = "binary.t";
		
		MockProducer<String, byte[]> mocked = new MockProducer<String, byte[]>(true,
				new StringSerializer(), new ByteArraySerializer());
		
		try(CloudEventsKafkaProducer<String, AttributesImpl, Much> 
			ceProducer = new CloudEventsKafkaProducer<>(binary(), mocked)){
			// act
			RecordMetadata metadata = 
				ceProducer.send(new ProducerRecord<>(topic, ce)).get();
			
			log.info("Producer metadata {}", metadata);
			
			assertFalse(mocked.history().isEmpty());
			mocked.history().forEach(actual -> {
				// assert
				assertNotNull(actual);
				Header traceparent = 
					actual.headers().lastHeader("traceparent");
				
				assertNotNull(traceparent);
				assertEquals("0", DESERIALIZER
						.deserialize(null, traceparent.value()));
				
				Header tracestate = 
						actual.headers().lastHeader("tracestate");
				assertNotNull(tracestate);
				assertEquals("congo=4", DESERIALIZER
						.deserialize(null, tracestate.value()));
			});
		}
	}
}
