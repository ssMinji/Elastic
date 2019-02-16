package test;

import java.io.IOException;

import org.sweble.wikitext.engine.EngineException;
import org.sweble.wikitext.parser.parser.LinkTargetException;

import com.google.gson.Gson;

public class Jsontest {

	public static class TestDTO {
		private String title;
		private String body;
		private double score;

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getBody() {
			return body;
		}

		public void setBody(String body) {
			this.body = body;
		}

		public double getScore() {
			return score;
		}

		public void setScore(double score) {
			this.score = score;
		}
		
		@Override
		public String toString() {
			return title + body;
		}

	}



	public static void main(String[] args) throws IOException, LinkTargetException, EngineException {
		Gson gson = new Gson();
		TestDTO d = new TestDTO();
		d.setTitle("newtitle");
		d.setBody("112352534ty4wgasfgsad");
		d.setScore(5.99d);
		
		String exam = gson.toJson(d);
		System.out.println(exam);


		TestDTO dto = gson.fromJson(exam, TestDTO.class);
		System.out.println(dto.getTitle());
	}

}
