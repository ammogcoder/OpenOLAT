/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.modules.quality.manager;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.translator.Translator;
import org.olat.core.helpers.Settings;
import org.olat.core.id.Identity;
import org.olat.core.id.User;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.i18n.I18nManager;
import org.olat.core.util.mail.MailBundle;
import org.olat.core.util.mail.MailManager;
import org.olat.core.util.mail.MailTemplate;
import org.olat.core.util.mail.MailerResult;
import org.olat.modules.forms.EvaluationFormParticipation;
import org.olat.modules.forms.RubricRating;
import org.olat.modules.forms.RubricStatistic;
import org.olat.modules.forms.ui.EvaluationFormFormatter;
import org.olat.modules.quality.QualityDataCollection;
import org.olat.modules.quality.QualityDataCollectionStatus;
import org.olat.modules.quality.QualityDataCollectionTopicType;
import org.olat.modules.quality.QualityDataCollectionView;
import org.olat.modules.quality.QualityDataCollectionViewSearchParams;
import org.olat.modules.quality.QualityExecutorParticipation;
import org.olat.modules.quality.QualityExecutorParticipationSearchParams;
import org.olat.modules.quality.QualityReminder;
import org.olat.modules.quality.model.QualityMailTemplateBuilder;
import org.olat.modules.quality.ui.QualityMainController;
import org.olat.modules.quality.ui.QualityUIContextsBuilder;
import org.olat.modules.quality.ui.QualityUIContextsBuilder.Attribute;
import org.olat.modules.quality.ui.QualityUIContextsBuilder.KeyValue;
import org.olat.modules.quality.ui.QualityUIContextsBuilder.UIContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * Initial date: 10.07.2018<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
@Service
class QualityMailing {

	private static final OLog log = Tracing.createLoggerFor(QualityMailing.class);
	
	private static final Collection<QualityDataCollectionStatus> STATUS_FILTER = Arrays.asList(
			QualityDataCollectionStatus.READY,
			QualityDataCollectionStatus.RUNNING,
			QualityDataCollectionStatus.FINISHED);
	
	@Autowired
	private QualityParticipationDAO participationDao;
	@Autowired
	private QualityDataCollectionDAO dataCollectionDao;

	void sendReminderMail(QualityReminder reminder, QualityReminder invitation, EvaluationFormParticipation participation) {
		if (participation.getExecutor() != null) {
			Identity executor = participation.getExecutor();
			MailTemplate template = createReminderMailTemplate(reminder, invitation, executor);
			
			MailerResult result = new MailerResult();
			MailManager mailManager = CoreSpringFactory.getImpl(MailManager.class);
			MailBundle bundle = mailManager.makeMailBundle(null, executor, template, null, null, result);
			if(bundle != null) {
				result = mailManager.sendMessage(bundle);
				if (result.isSuccessful()) {
					log.info(MessageFormat.format("{0} for quality data collection [key={1}] sent to {2}",
							reminder.getType().name(), reminder.getDataCollection().getKey(), executor));
				} else {
					log.warn(MessageFormat.format("Sending {0} for quality data collection [key={1}] to {2} failed: {3}",
							reminder.getType().name(), reminder.getDataCollection().getKey(), executor, result.getErrorMessage()));
				}
			}
		}
	}

	private MailTemplate createReminderMailTemplate(QualityReminder reminder, QualityReminder invitationReminder, Identity executor) {
		Locale locale = I18nManager.getInstance().getLocaleOrDefault(executor.getUser().getPreferences().getLanguage());
		Translator translator = Util.createPackageTranslator(QualityMainController.class, locale);
		
		QualityExecutorParticipationSearchParams searchParams = new QualityExecutorParticipationSearchParams();
		searchParams.setExecutorRef(executor);
		searchParams.setDataCollectionRef(reminder.getDataCollection());
		searchParams.setDataCollectionStatus(STATUS_FILTER);
		List<QualityExecutorParticipation> participations = participationDao.loadExecutorParticipations(translator, searchParams , 0, -1);
		QualityExecutorParticipation participation = null;
		if (!participations.isEmpty()) {
			participation = participations.get(0);
		}
		
		String subject = translator.translate(reminder.getType().getSubjectI18nKey());
		String body = translator.translate(reminder.getType().getBodyI18nKey());
		QualityMailTemplateBuilder mailBuilder = QualityMailTemplateBuilder.builder(subject, body, locale);
		
		User user = executor.getUser();
		mailBuilder.withExecutor(user);
		
		if (participation != null) {
			mailBuilder.withStart(participation.getStart())
					.withDeadline(participation.getDeadline())
					.withTopic(participation.getTopic())
					.withTitle(participation.getTitle())
					.withPreviousTitle(participation.getPreviousTitle());
			
			String custom = translator.translate(QualityDataCollectionTopicType.CUSTOM.getI18nKey());
			if (!custom.equals(participation.getTranslatedTopicType())) {
				mailBuilder.withTopicType(participation.getTranslatedTopicType());
			}
			
			String seriePorition = participation.getPreviousTitle() != null
					? translator.translate("reminder.serie.followup")
					: translator.translate("reminder.serie.primary");
			mailBuilder.withSeriePosition(seriePorition);
			
			Long participationKey = participation.getParticipationRef().getKey();
			String url = getParticipationUrl(participationKey);
			mailBuilder.withUrl(url);
			
			String surveyContext = createParticipationContext(participation, locale);
			mailBuilder.withContext(surveyContext);
		}
		
		mailBuilder.withInvitation(invitationReminder.getSendDone());
		
		return mailBuilder.build();
	}

	private String getParticipationUrl(Long participationKey) {
		StringBuilder url = new StringBuilder();
		url.append(Settings.getServerContextPathURI());
		url.append("/url/QualitySite/0/quality/0/my/0/");
		if (participationKey != null) {
			url.append("execution/");
			url.append(participationKey);
		}
		return url.toString();
	}

	private String createParticipationContext(QualityExecutorParticipation participation, Locale locale) {
		StringBuilder sb = new StringBuilder();
		List<UIContext> uiContexts = QualityUIContextsBuilder.builder(participation, locale)
				.addAttribute(Attribute.ROLE)
				.addAttribute(Attribute.COURSE)
				.addAttribute(Attribute.CURRICULUM_ELEMENTS)
				.addAttribute(Attribute.TAXONOMY_LEVELS)
				.build()
				.getUiContexts();
		for (UIContext uiContext : uiContexts) {
			sb.append("<br/>");
			for (KeyValue kv : uiContext.getKeyValues()) {
				sb.append("<br/>").append(kv.getKey()).append(": ").append(kv.getValue());
			}
		}
		return sb.toString();
	}

	void sendReportAccessEmail(QualityDataCollection dataCollection, Collection<Identity> recipients,
			List<RubricStatistic> rubricStatistics) {
		for (Identity recipient : recipients) {
			sendReportAccessEmail(dataCollection, recipient, rubricStatistics);
		}
		
	}
	
	private void sendReportAccessEmail(QualityDataCollection dataCollection, Identity recipient, List<RubricStatistic> rubricStatistics) {
		MailTemplate template = createReportAccessMailTemplate(dataCollection, recipient, rubricStatistics);
		
		MailerResult result = new MailerResult();
		MailManager mailManager = CoreSpringFactory.getImpl(MailManager.class);
		MailBundle bundle = mailManager.makeMailBundle(null, recipient, template, null, null, result);
		if(bundle != null) {
			result = mailManager.sendMessage(bundle);
			if (result.isSuccessful()) {
				log.info("Report access email send");
				log.info(MessageFormat.format("Report access email for quality data collection [key={0}] sent to {1}",
						dataCollection.getKey(), recipient));
			} else {
				log.warn(MessageFormat.format("Sending report access email for quality data collection [key={0}] to {1} failed: {2}",
						dataCollection.getKey(), recipient, result.getErrorMessage()));
			}
		}
	}

	private MailTemplate createReportAccessMailTemplate(QualityDataCollection dataCollection, Identity recipient,
			List<RubricStatistic> rubricStatistics) {
		Locale locale = I18nManager.getInstance().getLocaleOrDefault(recipient.getUser().getPreferences().getLanguage());
		Translator translator = Util.createPackageTranslator(QualityMainController.class, locale);
		
		String subject = translator.translate("report.access.email.subject");
		String body = translator.translate("report.access.email.body");
		QualityMailTemplateBuilder mailBuilder = QualityMailTemplateBuilder.builder(subject, body, locale);
		
		User user = recipient.getUser();
		mailBuilder.withExecutor(user);
		
		QualityDataCollectionViewSearchParams searchParams = new QualityDataCollectionViewSearchParams();
		searchParams.setDataCollectionRef(dataCollection);
		List<QualityDataCollectionView> dataCollectionViews = dataCollectionDao.loadDataCollections(translator, searchParams, 0, -1);
		
		if (!dataCollectionViews.isEmpty()) {
			QualityDataCollectionView dataCollectionView = dataCollectionViews.get(0);
			mailBuilder.withStart(dataCollectionView.getStart())
					.withDeadline(dataCollectionView.getDeadline())
					.withTopic(dataCollectionView.getTopic())
					.withTitle(dataCollectionView.getTitle())
					.withPreviousTitle(dataCollectionView.getPreviousTitle());
			
			String custom = translator.translate(QualityDataCollectionTopicType.CUSTOM.getI18nKey());
			if (!custom.equals(dataCollectionView.getTranslatedTopicType())) {
				mailBuilder.withTopicType(dataCollectionView.getTranslatedTopicType());
			}
			
			String seriePorition = dataCollectionView.getPreviousTitle() != null
					? translator.translate("reminder.serie.followup")
					: translator.translate("reminder.serie.primary");
			mailBuilder.withSeriePosition(seriePorition);
		}
		
		String url = getReportUrl(dataCollection.getKey());
		mailBuilder.withUrl(url);
		
		String surveyContext = createDatCollectionContext(dataCollection, locale);
		mailBuilder.withContext(surveyContext);
		
		String result = getResult(translator, rubricStatistics);
		mailBuilder.withResult(result);
		
		return mailBuilder.build();
	}
	
	private String getReportUrl(Long dataCollectionKey) {
		StringBuilder url = new StringBuilder();
		url.append(Settings.getServerContextPathURI());
		url.append("/url/QualitySite/0/quality/0/datacollections/0/");
		if (dataCollectionKey != null) {
			url.append("datacollection/");
			url.append(dataCollectionKey);
			url.append("/report/0/");
		}
		return url.toString();
	}
	
	private String createDatCollectionContext(QualityDataCollection dataCollection, Locale locale) {
		StringBuilder sb = new StringBuilder();
		List<UIContext> uiContexts = QualityUIContextsBuilder.builder(dataCollection, locale)
				.addAttribute(Attribute.ROLE)
				.addAttribute(Attribute.COURSE)
				.addAttribute(Attribute.CURRICULUM_ELEMENTS)
				.addAttribute(Attribute.TAXONOMY_LEVELS)
				.build()
				.getUiContexts();
		for (UIContext uiContext : uiContexts) {
			sb.append("<br/>");
			for (KeyValue kv : uiContext.getKeyValues()) {
				sb.append("<br/>").append(kv.getKey()).append(": ").append(kv.getValue());
			}
		}
		return sb.toString();
	}
	
	private String getResult(Translator translator, List<RubricStatistic> rubricStatistics) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < rubricStatistics.size(); i++) {
			RubricStatistic rubricStatistic = rubricStatistics.get(i);
			
			String rubricName = rubricStatistic.getRubric().getName();
			String rubricTranslatedName = StringHelper.containsNonWhitespace(rubricName)
					? translator.translate("report.access.email.rubric.name", new String[] {rubricName})
					: "";
			RubricRating rating = rubricStatistic.getTotalStatistic().getRating();
			String translatedRating = translator.translate(getRatingI18n(rating));
			
			String[] args = {
					String.valueOf(i + 1),                                    // rubric index
					rubricTranslatedName,                                     // rubric name
					EvaluationFormFormatter.formatDouble(
							rubricStatistic.getTotalStatistic().getAvg()),    // average
					translatedRating                                          // rating
			};
			
			sb.append("<br/>");
			sb.append(translator.translate("report.access.email.rubric.rating", args));
		}
		return sb.toString();
	}

	private String getRatingI18n(RubricRating rating) {
		switch (rating) {
		case SUFFICIENT: 
			return "report.access.rating.sufficient";
		case NEUTRAL: 
			return "report.access.rating.neutral";
		case INSUFFICIENT: 
			return "report.access.rating.insufficient";
		default: 
			return "report.access.rating.not.rated";
		}
	}

}
