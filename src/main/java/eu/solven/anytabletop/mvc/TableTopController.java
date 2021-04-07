package eu.solven.anytabletop.mvc;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
public class TableTopController {

	@RequestMapping("/")
	public String index() {
		return "Greetings from Spring Boot!";
	}

}