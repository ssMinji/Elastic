package test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

public class Elastictest {

	public static void main(String[] args) throws IOException {

		@SuppressWarnings("resource")
		RestHighLevelClient client = new RestHighLevelClient(RestClient
				.builder(new HttpHost("54.180.187.104", 9200, "http"), new HttpHost("54.180.187.104", 9201, "http")));

		Map<String, Object> jsonMap = new HashMap<String, Object>();
		jsonMap.put("title", "kimchy");
		jsonMap.put("score", 1.20231);
		jsonMap.put("description", "trying out Elasticsearch");

		IndexRequest indexRequest = new IndexRequest("pagerank", "page", "2").source(jsonMap);
//      GetRequest getRequest = new GetRequest("pagerank", "page", "1");

		System.out.println(client.ping());

//      MultiGetRequest request = new MultiGetRequest();
//      MultiGetRequest str = request.add(new MultiGetRequest.Item(
//             "pagerank",         
//             "page",          
//             "1").storedFields("title"));  
		IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
//      String value = item.getResponse().getField("title").getValue(); 

		System.out.println(indexResponse);

	}
}