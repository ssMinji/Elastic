package test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Index;


public class JestTest {

	public static void main(String[] args) throws IOException {
      
		Map<String, Object> jsonMap = new HashMap<String, Object>();
		jsonMap.put("title", "22222222222222s");
		jsonMap.put("score", 1.20231);
		jsonMap.put("description", "123123123123123123123123");

		
		JestClientFactory factory = new JestClientFactory();
		factory.setHttpClientConfig(new HttpClientConfig.Builder("54.180.187.104:9200").multiThreaded(true).build());
		JestClient client = factory.getObject();
		Index index = new Index.Builder(jsonMap).index("pagerank").type("page").build();
		System.out.println(client.execute(index));

	}
}
