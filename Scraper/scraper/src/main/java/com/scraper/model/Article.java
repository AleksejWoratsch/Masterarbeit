package com.scraper.model;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public class Article implements Serializable {
	private static final long serialVersionUID = 1L;

	private Publisher publisher;
	private Date date;
	private String author;
	private String topics;
	private String headline;
	private String teaser;
	private String content;
	private String url;

	public Article(Publisher publisher, String url) {
		super();
		// TODO Auto-generated constructor stub
		this.publisher = publisher;
		this.url = url;
		this.topics = "N/A";
	}

	public Publisher getPublisher() {
		return publisher;
	}

	public void setPublisher(Publisher publisher) {
		this.publisher = publisher;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getTopics() {
		return topics;
	}

	public void setTopics(String topics) {
		this.topics = topics;
	}

	public String getHeadline() {
		return headline;
	}

	public void setHeadline(String headline) {
		this.headline = headline;
	}

	public String getTeaser() {
		return teaser;
	}

	public void setTeaser(String teaser) {
		this.teaser = teaser;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public String toString() {
		return "Article [publisher=" + publisher + ", date=" + date + ", author=" + author + ", topics=" + topics
				+ ", headline=" + headline + ", teaser=" + teaser + ", content=" + content + ", url=" + url + "]";
	}

	public void toCSV(String filePath) throws IOException {

		OutputStream outputStream = new FileOutputStream(filePath, true);
		Writer outputStreamWriter = new OutputStreamWriter(outputStream, "UTF-8");
		CSVPrinter printer = new CSVPrinter(outputStreamWriter, CSVFormat.DEFAULT);
		
		printer.printRecord(this.getPublisher().toString(),
				new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(this.getDate()), this.getAuthor(), this.getTopics(),
				this.getHeadline(), this.getTeaser(), this.getContent(), this.getUrl());
		printer.flush();
		printer.close();
	}

}
