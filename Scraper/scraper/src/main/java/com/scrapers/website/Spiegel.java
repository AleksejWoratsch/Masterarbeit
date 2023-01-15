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

public class Spiegel {
	final static Logger logger = Logger.getLogger(Spiegel.class);
	public final static String[] CSV_HEADER = { "PUBLISHER", "DATE", "AUTHOR", "TOPICS", "HEADLINE", "TEASER",
			"CONTENT", "URL" };
	private static final Publisher THIS_PUBLISHER = Publisher.SPIEGEL_DE;
	public final static String BROWSER_USER_AGENT_HEADER = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36";

	private static final SimpleDateFormat dateCSVExportFormat = new SimpleDateFormat("dd/MM/yyyy");

	
	private WebClient browser;

	private String startDate;

	private String outputFilePath;
	public String outputFolderPath;

	

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Spiegel application = new Spiegel();
		application.outputFolderPath = System.getProperty("user.dir");
		// format yyyy/MM/dd
		application.startDate = "2001/12/01";
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

			java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
			java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);

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

			logger.info("completed");

		} catch (Exception ex) {
			logger.error("method start error : ", ex);
		}
	}

	public void browseArchive(Calendar iterateDate) {
		try {

			while (iterateDate.before(Calendar.getInstance())) {
				try {

					iterateDate.add(Calendar.DAY_OF_MONTH, 1);
					String strDateToExtract = new SimpleDateFormat("dd.MM.yyyy").format(iterateDate.getTime());

					String listArticlesPageURL = "https://www.spiegel.de/nachrichtenarchiv/artikel-" + strDateToExtract
							+ ".html";

					logger.info(listArticlesPageURL);

					HtmlPage articlesPage = this.browser.getPage(listArticlesPageURL);
					List<DomElement> listArticleAElement = articlesPage.getByXPath("//article//a");
					for (DomElement articleAElement : listArticleAElement) {
						Article article = new Article(THIS_PUBLISHER, articleAElement.getAttribute("href"));
						article.setDate(iterateDate.getTime());
						extractArticle(article);
					}

				} catch (Exception ex) {
					logger.warn("issue : ", ex);
				}
			}

		} catch (Exception ex) {
			logger.error("method browseArchive error : ", ex);
		}
	}

	public void extractArticle(Article article) {
		try {

			logger.info("    " + article.getUrl());

			HtmlPage articlePage = this.browser.getPage(article.getUrl());

			
			// Thread.sleep(randomInRange(500, 700));

			DomElement titleElement = articlePage
					.getFirstByXPath("//header[@data-area='intro']//span[@class='align-middle']");
			if (titleElement != null)
				article.setHeadline(titleElement.getTextContent().trim());

			DomElement authorElement = articlePage
					.getFirstByXPath("//a[starts-with(@href,'https://www.spiegel.de/impressum/autor')]");
			if (authorElement != null)
				article.setAuthor(authorElement.getAttribute("title"));

			DomElement topicElement = articlePage.getFirstByXPath("//span[@data-headerbar-el='logo']/a");
			if (topicElement != null)
				article.setTopics(topicElement.getAttribute("title"));

			DomElement teaserElement = articlePage.getFirstByXPath("//meta[@name='description']");
			if (teaserElement != null)
				article.setTeaser(teaserElement.getAttribute("content"));

			DomElement articleBodyElement = articlePage.getFirstByXPath("//div[@data-area='body']");
			if (articleBodyElement != null)
				article.setContent(StringUtils.normalizeSpace(articleBodyElement.asText()).trim());

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

	private int randomInRange(int min, int max) {
		return new Random().nextInt((max - min) + 1) + min;
	}

}
