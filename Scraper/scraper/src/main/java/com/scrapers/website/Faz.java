package com.scrapers.website;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.logging.Level;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.scraper.model.Article;
import com.scraper.model.Publisher;

public class Faz {

	final static Logger logger = Logger.getLogger(Faz.class);
	public final static String[] CSV_HEADER = { "PUBLISHER", "DATE", "AUTHOR", "TOPICS", "HEADLINE", "TEASER",
			"CONTENT", "URL" };
	private static final Publisher THIS_PUBLISHER = Publisher.FAZ_NET;

	private static final SimpleDateFormat dateCSVExportFormat = new SimpleDateFormat("dd/MM/yyyy");

	/*
	 * HTML-UNIT benötigt für die Requests einen HTTP Header, hier wird ein Browser
	 * mitgegeben,
	 * damit der angefragte Server Device erkennt
	 * 
	 */

	public final static String BROWSER_USER_AGENT_HEADER = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36";

	private WebClient browser;
	public String startDate;
	private String outputFilePath;
	public String outputFolderPath;

	public static void main(String[] args) {

		Faz application = new Faz();
		application.outputFolderPath = System.getProperty("user.dir");

		// format yyyy/MM/dd
		application.startDate = "2001/01/01";
		application.start();
	}

	public void start() {
		try {

			SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
			logger.info("start " + THIS_PUBLISHER + " extraction : " + formater.format(new Date()));
			logger.info("start Date" + this.startDate + "\n");

			// setze CSV output
			this.outputFilePath = this.outputFolderPath + System.getProperty("file.separator") + THIS_PUBLISHER.name()
					+ "_" + formater.format(new Date()) + ".csv";

			// neues CSV Erstellen, erhält Columns vom Array CSV_HEADER
			// Verwendung des Paths, welcher zuvor erstellt wurde
			FileWriter out = new FileWriter(this.outputFilePath, true);
			CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(CSV_HEADER));
			printer.flush();
			printer.close();

			// Unwichtige Browser-Logs nicht loggen
			java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
			java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);

			logger.info("initialize virtual browser ...");
			this.browser = new WebClient();
			// Optionen für besseres Handling
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
			// Calendar.getInstance() return derzeitiges System-Datum
			// so lange iterateDate < derzeitiges System-Datum, suche weiter, nach jedem
			// Loop füge ein Tag hinzu
			while (iterateDate.before(Calendar.getInstance())) {
				try {

					iterateDate.add(Calendar.DAY_OF_MONTH, 1);

					String strDateToExtract = new SimpleDateFormat("yyyy-MMMM-dd", Locale.GERMANY)
							.format(iterateDate.getTime());

					String listArticlesPageURL = "https://www.faz.net/artikel-chronik/nachrichten-"
							+ strDateToExtract.toLowerCase() + "/";

					logger.info(listArticlesPageURL);

					HtmlPage articlesPage = this.browser.getPage(listArticlesPageURL);

					List<DomElement> listAElement = articlesPage.getByXPath(
							"//div[@id='FAZContentLeftInner']/div[starts-with(@class,'Teaser')]/a[@class='TeaserHeadLink']");
					for (DomElement aElement : listAElement) {
						String articleURL = "https://www.faz.net" + aElement.getAttribute("href");
						// über den konstruktur die artikel url setzen
						Article article = new Article(THIS_PUBLISHER, articleURL);
						article.setDate(iterateDate.getTime());
						// extrahieren
						extractArticle(article);
					}

					logger.info("");

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

			/*
			 * Ucomment für Delay beim scrapen
			 */

			// MIN , MAX values in milliseconds
			// Thread.sleep(randomInRange(500, 700));

			DomElement titleElement = articlePage.getFirstByXPath("//title");
			if (titleElement != null)
				article.setHeadline(titleElement.getTextContent().trim());

			DomElement authorElement = articlePage.getFirstByXPath("//span[@class='atc-MetaAuthor']");
			if (authorElement != null)
				article.setAuthor(StringUtils.normalizeSpace(authorElement.getTextContent()).trim());

			DomElement topicElement = articlePage.getFirstByXPath("//div[@class='gh-Ressort_LinkInner']");
			if (topicElement != null)
				article.setTopics(StringUtils.normalizeSpace(topicElement.getTextContent()).trim());

			DomElement teaserElement = articlePage.getFirstByXPath("//p[@class='atc-IntroText']");
			if (teaserElement != null)
				article.setTeaser(StringUtils.normalizeSpace(teaserElement.getTextContent()).trim());
			// body besteht aus mehreren p elementen

			String body = "";
			List<DomElement> listPElement = articlePage.getByXPath("//p[contains(@class,'atc-TextParagraph')]");
			for (DomElement pElement : listPElement)
				body = body + " " + pElement.getTextContent();
			body = StringUtils.normalizeSpace(body).trim();
			article.setContent(body);

			writeToCSV(article);

		} catch (Exception ex) {
			logger.error("method extractArticle error : ", ex);
		}
	}
	// CSV mit den Objekten befüllen, hier wird Commons CSV libary verwendet

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

	private int randomInRange(int min, int max) {
		return new Random().nextInt((max - min) + 1) + min;
	}
}
