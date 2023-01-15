package com.scrapers.website;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.scraper.model.Article;
import com.scraper.model.Publisher;

public class Bild {
	final static Logger logger = Logger.getLogger(Bild.class);
	public final static String[] CSV_HEADER = { "PUBLISHER", "DATE", "AUTHOR", "TOPICS", "HEADLINE", "TEASER",
			"CONTENT", "URL" };
	private static final Publisher THIS_PUBLISHER = Publisher.BILD_DE;

	private static final SimpleDateFormat dateCSVExportFormat = new SimpleDateFormat("dd/MM/yyyy");

	public final static String BROWSER_USER_AGENT_HEADER = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36";

	private WebClient browser;
	public String startDate;

	private String outputFilePath;
	public String outputFolderPath;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Bild application = new Bild();
		
		application.outputFolderPath = System.getProperty("user.dir");
		// format  yyyy/MM/dd
		application.startDate = "2006/12/01";
		application.start();
	}

	public void start() {
		try {

			SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
			logger.info("start " + THIS_PUBLISHER + " extraction : " + formater.format(new Date()));

			this.outputFilePath = this.outputFolderPath + System.getProperty("file.separator") + THIS_PUBLISHER.name()
					+ "_" + formater.format(new Date()) + ".csv";

			FileWriter out = new FileWriter(this.outputFilePath, true);
			CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(CSV_HEADER));
			printer.flush();
			printer.close();

			java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
			System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

			logger.info("initialize virtual browser ...");
			this.browser = new WebClient();
			this.browser.getOptions().setCssEnabled(false);
			this.browser.getOptions().setJavaScriptEnabled(false);
			this.browser.getOptions().setThrowExceptionOnFailingStatusCode(false);
			this.browser.getOptions().setThrowExceptionOnScriptError(false);
			this.browser.getOptions().setTimeout(120 * 1000);
			this.browser.getOptions().setRedirectEnabled(true);
			this.browser.getOptions().setUseInsecureSSL(true);
			this.browser.addRequestHeader("user-agent", BROWSER_USER_AGENT_HEADER);

			Calendar iterateDate = Calendar.getInstance();
			iterateDate.setTime(new SimpleDateFormat("yyyy/MM/dd").parse(this.startDate));
			iterateDate.add(Calendar.DAY_OF_MONTH, -1);

			browseArchive(iterateDate);

		} catch (Exception ex) {
			logger.error("method start error : ", ex);
		}
	}

	public void browseArchive(Calendar iterateDate) {
		try {

			while (iterateDate.before(Calendar.getInstance())) {
				try {

					iterateDate.add(Calendar.DAY_OF_MONTH, 1);
					String strDateToExtract = new SimpleDateFormat("yyyy/M/d").format(iterateDate.getTime());

					String listArticlesPageURL = "https://www.bild.de/archive/" + strDateToExtract + "/index.html";
					logger.info(listArticlesPageURL);

					HtmlPage articlesPage = this.browser.getPage(listArticlesPageURL);
					List<DomElement> listAElement = articlesPage
							.getByXPath("//p[preceding-sibling::h2[1][contains(text(),'Alle Artikel')]]/a");
					for (DomElement aElement : listAElement) {

						String articleURL = "https://www.bild.de" + aElement.getAttribute("href");
						if (articleURL.contains("bild-plus") == false) {
							Article article = new Article(THIS_PUBLISHER, articleURL);
							article.setDate(iterateDate.getTime());
							extractArticle(article);
						}
					}

				} catch (Exception ex) {
					logger.warn("issue on : ", ex);
				}
			}

			logger.info("completed");

		} catch (Exception ex) {
			logger.error("method browseArchive error : ", ex);
		}
	}

	public void extractArticle(Article article) {
		try {

			logger.info("          " + article.getUrl());

			HtmlPage articlePage = this.browser.getPage(article.getUrl());

			DomElement scriptElement = articlePage.getFirstByXPath(
					"//script[@type='application/ld+json' and (contains(text(),'Article') or contains(text(),'NewsArticle'))]");
			if (scriptElement != null) {
				String content = scriptElement.getTextContent();

				JsonObject jsonPayload = new JsonParser().parse(new JsonReader(new StringReader(content)))
						.getAsJsonObject();
				if (jsonPayload.has("headline") && jsonPayload.get("headline").isJsonNull() == false)
					article.setHeadline(jsonPayload.get("headline").getAsString());

				if (jsonPayload.has("description") && jsonPayload.get("description").isJsonNull() == false)
					article.setTeaser(jsonPayload.get("description").getAsString());

				if (jsonPayload.has("articleBody") && jsonPayload.get("articleBody").isJsonNull() == false)
					article.setContent(jsonPayload.get("articleBody").getAsString());

				if (jsonPayload.has("author") && jsonPayload.get("author").isJsonNull() == false
						&& jsonPayload.get("author").isJsonArray()) {
					for (JsonElement jsElement : jsonPayload.get("author").getAsJsonArray()) {
						if (jsElement.getAsJsonObject().has("name")
								&& jsElement.getAsJsonObject().get("name").isJsonNull() == false) {
							article.setAuthor(jsElement.getAsJsonObject().get("name").getAsString());
							break;
						}
					}
				}

				// REGEX um 'topic' aus der url zu extrahieren
				Pattern pattern = Pattern.compile("bild.de/(.*?)/");
				Matcher matcher = pattern.matcher(StringUtils.normalizeSpace(content).trim());
				if (matcher.find())
					article.setTopics(StringUtils.normalizeSpace(matcher.group(1)).trim());

			}

			writeToCSV(article);

		} catch (Exception ex) {
			logger.error("method extractArticle error : ", ex);
		}
	}

	public void writeToCSV(Article article) throws IOException {
		OutputStream outputStream = new FileOutputStream(this.outputFilePath, true);
		Writer outputStreamWriter = new OutputStreamWriter(outputStream, "UTF-8");
		CSVPrinter printer = new CSVPrinter(outputStreamWriter, CSVFormat.DEFAULT);
		printer.printRecord(article.getPublisher().toString(), dateCSVExportFormat.format(article.getDate()),
				article.getAuthor(), article.getTopics(), article.getHeadline(), article.getTeaser(),
				article.getContent(), article.getUrl());
		printer.flush();
		printer.close();
	}

}
