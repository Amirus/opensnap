package opensnap;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import opensnap.domain.User;
import opensnap.service.UserService;

import org.bson.types.ObjectId;
import org.mongodb.MongoClientOptions;
import org.mongodb.MongoClientURI;
import org.mongodb.async.MongoClient;
import org.mongodb.async.MongoClients;
import org.mongodb.async.MongoDatabase;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "opensnap")
public class Application extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(new Object[]{Application.class}, args);
	}

	@Bean
	public InitializingBean populateTestData(UserService userService) {
		return () -> {
			if (!userService.exists("anonymous").get()) {
				userService.create(new User("anonymous", "jdqsjkdjsqkjd", Arrays.asList("ANONYMOUS")));
				userService.create(new User("seb", "s3b", Arrays.asList("USER", "ADMIN")));
				userService.create(new User("adeline", "ad3l1n3", Arrays.asList("USER")));
				userService.create(new User("johanna", "j0hanna", Arrays.asList("USER")));
				userService.create(new User("michel", "m1ch3l", Arrays.asList("USER")));
			}
		};
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new ShaPasswordEncoder(256);
	}

	@Bean
	public MongoClient mongoClient() throws UnknownHostException {
		return MongoClients.create(new MongoClientURI("mongodb://localhost:27017/"), MongoClientOptions.builder().build());
	}

	@Bean
	public MongoDatabase mongoDatabase(MongoClient mongoClient) {
		return mongoClient.getDatabase("opensnap");
	}

	@Primary
	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		SimpleModule module = new SimpleModule();
		module.addSerializer(ObjectId.class, new ObjectIdJsonSerializer());
		module.addDeserializer(ObjectId.class, new ObjectIdJsonDeserializer());
		mapper.registerModule(module);
		return mapper;
	}

	public class ObjectIdJsonSerializer extends JsonSerializer<ObjectId> {
		@Override
		public void serialize(ObjectId o, JsonGenerator j, SerializerProvider s) throws IOException {
			if (o == null) {
				j.writeNull();
			} else {
				j.writeStartObject();
				j.writeStringField("$oid", o.toString());
				j.writeEndObject();
			}
		}
	}

	public class ObjectIdJsonDeserializer extends JsonDeserializer<ObjectId> {
		@Override
		public ObjectId deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
			ObjectCodec oc = jp.getCodec();
			JsonNode node = oc.readTree(jp);
			if(node.isNull() || !node.has("$oid") || node.get("$oid").isNull()) {
				return null;
			}
			return new ObjectId(node.get("$oid").textValue());
		}
	}
}



