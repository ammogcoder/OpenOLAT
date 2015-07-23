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
package org.olat.course.assessment.manager;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.olat.core.CoreSpringFactory;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.activity.StringResourceableType;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.StringHelper;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.mail.MailerResult;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentChangedEvent;
import org.olat.course.assessment.AssessmentLoggingAction;
import org.olat.course.assessment.AssessmentManager;
import org.olat.course.assessment.EfficiencyStatementManager;
import org.olat.course.auditing.UserNodeAuditManager;
import org.olat.course.certificate.CertificateTemplate;
import org.olat.course.certificate.CertificatesManager;
import org.olat.course.certificate.model.CertificateInfos;
import org.olat.course.nodes.AssessableCourseNode;
import org.olat.course.nodes.CourseNode;
import org.olat.course.run.scoring.ScoreEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.assessment.AssessmentEntry;
import org.olat.modules.assessment.manager.AssessmentEntryDAO;
import org.olat.repository.RepositoryEntry;
import org.olat.util.logging.activity.LoggingResourceable;

/**
 * 
 * Initial date: 20.07.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class CourseAssessmentManagerImpl implements AssessmentManager {
	
	private static final Float FLOAT_ZERO = new Float(0);
	private static final Integer INTEGER_ZERO = new Integer(0);
	
	private final RepositoryEntry courseEntry;
	private final CertificatesManager certificatesManager;
	private final AssessmentEntryDAO courseNodeAssessmentDao;
	private final EfficiencyStatementManager efficiencyStatementManager;
	
	public CourseAssessmentManagerImpl(RepositoryEntry courseEntry) {
		this.courseEntry = courseEntry;
		certificatesManager = CoreSpringFactory.getImpl(CertificatesManager.class);
		courseNodeAssessmentDao = CoreSpringFactory.getImpl(AssessmentEntryDAO.class);
		efficiencyStatementManager = CoreSpringFactory.getImpl(EfficiencyStatementManager.class);
	}
	
	private AssessmentEntry getOrCreate(Identity assessedIdentity, CourseNode courseNode) {
		AssessmentEntry nodeAssessment = courseNodeAssessmentDao
				.loadAssessmentEntry(assessedIdentity, courseEntry, courseNode.getIdent());
		if(nodeAssessment == null) {
			nodeAssessment = courseNodeAssessmentDao
					.createCourseNodeAssessment(assessedIdentity, courseEntry,
							courseNode.getIdent(), courseNode.getReferencedRepositoryEntry());
		}
		return nodeAssessment;
	}

	@Override
	public List<AssessmentEntry> getAssessmentData(CourseNode courseNode) {
		return courseNodeAssessmentDao.loadAssessmentEntryBySubIdent(courseEntry, courseNode.getIdent());
	}

	@Override
	public void saveNodeAttempts(CourseNode courseNode, Identity identity, Identity assessedIdentity, Integer attempts) {
		ICourse course = CourseFactory.loadCourse(courseEntry.getOlatResource());
		
		AssessmentEntry nodeAssessment = getOrCreate(assessedIdentity, courseNode);
		nodeAssessment.setAttempts(attempts);
		courseNodeAssessmentDao.updateCourseNodeAssessment(nodeAssessment);

		//node log
		UserNodeAuditManager am = course.getCourseEnvironment().getAuditManager();
		am.appendToUserNodeLog(courseNode, identity, assessedIdentity, "ATTEMPTS set to: " + String.valueOf(attempts));

		// notify about changes
		AssessmentChangedEvent ace = new AssessmentChangedEvent(AssessmentChangedEvent.TYPE_ATTEMPTS_CHANGED, assessedIdentity);
		CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(ace, course);

		// user activity logging
		ThreadLocalUserActivityLogger.log(AssessmentLoggingAction.ASSESSMENT_ATTEMPTS_UPDATED, 
				getClass(), 
				LoggingResourceable.wrap(assessedIdentity), 
				LoggingResourceable.wrapNonOlatResource(StringResourceableType.qtiAttempts, "", String.valueOf(attempts)));	
	}

	@Override
	public void saveNodeComment(CourseNode courseNode, Identity identity, Identity assessedIdentity, String comment) {
		ICourse course = CourseFactory.loadCourse(courseEntry.getOlatResource());
		
		AssessmentEntry nodeAssessment = getOrCreate(assessedIdentity, courseNode);
		nodeAssessment.setComment(comment);
		courseNodeAssessmentDao.updateCourseNodeAssessment(nodeAssessment);
		
		// node log
		UserNodeAuditManager am = course.getCourseEnvironment().getAuditManager();
		am.appendToUserNodeLog(courseNode, identity, assessedIdentity, "COMMENT set to: " + comment);

		// notify about changes
		AssessmentChangedEvent ace = new AssessmentChangedEvent(AssessmentChangedEvent.TYPE_USER_COMMENT_CHANGED, assessedIdentity);
		CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(ace, course);

		// user activity logging
		ThreadLocalUserActivityLogger.log(AssessmentLoggingAction.ASSESSMENT_USERCOMMENT_UPDATED, 
				getClass(), 
				LoggingResourceable.wrap(assessedIdentity), 
				LoggingResourceable.wrapNonOlatResource(StringResourceableType.qtiUserComment, "", StringHelper.stripLineBreaks(comment)));	
	}

	@Override
	public void saveNodeCoachComment(CourseNode courseNode, Identity assessedIdentity, String comment) {
		ICourse course = CourseFactory.loadCourse(courseEntry.getOlatResource());
		
		AssessmentEntry nodeAssessment = getOrCreate(assessedIdentity, courseNode);
		nodeAssessment.setCoachComment(comment);
		courseNodeAssessmentDao.updateCourseNodeAssessment(nodeAssessment);
		
		// notify about changes
		AssessmentChangedEvent ace = new AssessmentChangedEvent(AssessmentChangedEvent.TYPE_COACH_COMMENT_CHANGED, assessedIdentity);
		CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(ace, course);

		// user activity logging
		ThreadLocalUserActivityLogger.log(AssessmentLoggingAction.ASSESSMENT_COACHCOMMENT_UPDATED, 
				getClass(), 
				LoggingResourceable.wrap(assessedIdentity), 
				LoggingResourceable.wrapNonOlatResource(StringResourceableType.qtiCoachComment, "", StringHelper.stripLineBreaks(comment)));	
	}

	@Override
	public void incrementNodeAttempts(CourseNode courseNode, Identity assessedIdentity, UserCourseEnvironment userCourseEnv) {
		ICourse course = CourseFactory.loadCourse(courseEntry.getOlatResource());
		
		AssessmentEntry nodeAssessment = getOrCreate(assessedIdentity, courseNode);
		int attempts = nodeAssessment.getAttempts() == null ? 1 :nodeAssessment.getAttempts().intValue() + 1;
		nodeAssessment.setAttempts(attempts);
		courseNodeAssessmentDao.updateCourseNodeAssessment(nodeAssessment);
		if(courseNode instanceof AssessableCourseNode) {
			// Update users efficiency statement
			efficiencyStatementManager.updateUserEfficiencyStatement(userCourseEnv);
		}
		
		// notify about changes
		AssessmentChangedEvent ace = new AssessmentChangedEvent(AssessmentChangedEvent.TYPE_ATTEMPTS_CHANGED, assessedIdentity);
		CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(ace, course);
		
		// user activity logging
		ThreadLocalUserActivityLogger.log(AssessmentLoggingAction.ASSESSMENT_ATTEMPTS_UPDATED, 
				getClass(), 
				LoggingResourceable.wrap(assessedIdentity), 
				LoggingResourceable.wrapNonOlatResource(StringResourceableType.qtiAttempts, "", String.valueOf(attempts)));
	}

	@Override
	public void incrementNodeAttemptsInBackground(CourseNode courseNode, Identity assessedIdentity, UserCourseEnvironment userCourseEnv) {
		ICourse course = CourseFactory.loadCourse(courseEntry.getOlatResource());
		
		AssessmentEntry nodeAssessment = getOrCreate(assessedIdentity, courseNode);
		int attempts = nodeAssessment.getAttempts() == null ? 1 :nodeAssessment.getAttempts().intValue() + 1;
		nodeAssessment.setAttempts(attempts);
		courseNodeAssessmentDao.updateCourseNodeAssessment(nodeAssessment);
		if(courseNode instanceof AssessableCourseNode) {
			// Update users efficiency statement
			efficiencyStatementManager.updateUserEfficiencyStatement(userCourseEnv);
		}
		
		// notify about changes
		AssessmentChangedEvent ace = new AssessmentChangedEvent(AssessmentChangedEvent.TYPE_ATTEMPTS_CHANGED, assessedIdentity);
		CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(ace, course);
	}
	
	@Override
	public void saveScoreEvaluation(CourseNode courseNode, Identity identity, Identity assessedIdentity,
			ScoreEvaluation scoreEvaluation, UserCourseEnvironment userCourseEnv,
			boolean incrementUserAttempts) {
		ICourse course = CourseFactory.loadCourse(courseEntry.getOlatResource());
		
		Float score = scoreEvaluation.getScore();
		Boolean passed = scoreEvaluation.getPassed();
		Long assessmentId = scoreEvaluation.getAssessmentID();

		AssessmentEntry nodeAssessment = getOrCreate(assessedIdentity, courseNode);
		if(score == null) {
			nodeAssessment.setScore(null);
		} else {
			nodeAssessment.setScore(new BigDecimal(Float.toString(score)));
		}
		nodeAssessment.setPassed(passed);
		nodeAssessment.setFullyAssessed(scoreEvaluation.getFullyAssessed());
		nodeAssessment.setAssessmentId(assessmentId);
		if(incrementUserAttempts) {
			int attempts = nodeAssessment.getAttempts() == null ? 1 :nodeAssessment.getAttempts().intValue() + 1;
			nodeAssessment.setAttempts(attempts);
		}
		nodeAssessment = courseNodeAssessmentDao.updateCourseNodeAssessment(nodeAssessment);
		
		
		if(courseNode instanceof AssessableCourseNode) {
			// Update users efficiency statement
			efficiencyStatementManager.updateUserEfficiencyStatement(userCourseEnv);
		}
		
		if(passed != null && passed.booleanValue() && course.getCourseConfig().isAutomaticCertificationEnabled()) {
			if(certificatesManager.isRecertificationAllowed(assessedIdentity, courseEntry)) {
				CertificateTemplate template = null;
				Long templateId = course.getCourseConfig().getCertificateTemplate();
				if(templateId != null) {
					template = certificatesManager.getTemplateById(templateId);
				}
				CertificateInfos certificateInfos = new CertificateInfos(assessedIdentity, score, passed);
				MailerResult result = new MailerResult();
				certificatesManager.generateCertificate(certificateInfos, courseEntry, template, result);
			}
		}
	}

	@Override
	public Float getNodeScore(CourseNode courseNode, Identity identity) {
		if (courseNode == null) {
			return FLOAT_ZERO; // return default value
		}
		
		AssessmentEntry nodeAssessment = courseNodeAssessmentDao
				.loadAssessmentEntry(identity, courseEntry, courseNode.getIdent());	
		if(nodeAssessment != null && nodeAssessment.getScore() != null) {
			return nodeAssessment.getScore().floatValue();
		}
		return FLOAT_ZERO;
	}

	@Override
	public String getNodeComment(CourseNode courseNode, Identity identity) {
		AssessmentEntry nodeAssessment = courseNodeAssessmentDao
				.loadAssessmentEntry(identity, courseEntry, courseNode.getIdent());	
		return nodeAssessment == null ? null : nodeAssessment.getComment();
	}

	@Override
	public String getNodeCoachComment(CourseNode courseNode, Identity identity) {
		AssessmentEntry nodeAssessment = courseNodeAssessmentDao
				.loadAssessmentEntry(identity, courseEntry, courseNode.getIdent());	
		return nodeAssessment == null ? null : nodeAssessment.getCoachComment();
	}

	@Override
	public Boolean getNodePassed(CourseNode courseNode, Identity identity) {
		if (courseNode == null) {
			return Boolean.FALSE; // return default value
		}
		
		AssessmentEntry nodeAssessment = courseNodeAssessmentDao
				.loadAssessmentEntry(identity, courseEntry, courseNode.getIdent());	
		return nodeAssessment == null ? null : nodeAssessment.getPassed();
	}

	@Override
	public Integer getNodeAttempts(CourseNode courseNode, Identity identity) {
		if(courseNode == null) return INTEGER_ZERO;
		
		AssessmentEntry nodeAssessment = courseNodeAssessmentDao
				.loadAssessmentEntry(identity, courseEntry, courseNode.getIdent());	
		return nodeAssessment == null ? INTEGER_ZERO : nodeAssessment.getAttempts();
	}

	@Override
	public Long getAssessmentID(CourseNode courseNode, Identity identity) {
		AssessmentEntry nodeAssessment = courseNodeAssessmentDao
				.loadAssessmentEntry(identity, courseEntry, courseNode.getIdent());	
		return nodeAssessment == null ? null : nodeAssessment.getAssessmentId();
	}

	@Override
	public Date getScoreLastModifiedDate(CourseNode courseNode, Identity identity) {
		if(courseNode == null) return null;
		AssessmentEntry nodeAssessment = courseNodeAssessmentDao
				.loadAssessmentEntry(identity, courseEntry, courseNode.getIdent());
		return nodeAssessment == null ? null : nodeAssessment.getLastModified();
	}

	@Override
	public Boolean getNodeFullyAssessed(CourseNode courseNode, Identity identity) {
		AssessmentEntry nodeAssessment = courseNodeAssessmentDao
				.loadAssessmentEntry(identity, courseEntry, courseNode.getIdent());	
		return nodeAssessment == null ? null : nodeAssessment.getFullyAssessed();
	}
	
	@Override
	public OLATResourceable createOLATResourceableForLocking(Identity assessedIdentity) {
		return OresHelper.createOLATResourceableInstance("AssessmentManager::Identity", assessedIdentity.getKey());
	}
	
	@Override
	public void registerForAssessmentChangeEvents(GenericEventListener gel, Identity identity) {
		CoordinatorManager.getInstance().getCoordinator().getEventBus().registerFor(gel, identity, courseEntry.getOlatResource());
	}

	@Override
	public void deregisterFromAssessmentChangeEvents(GenericEventListener gel) {
		CoordinatorManager.getInstance().getCoordinator().getEventBus().deregisterFor(gel, courseEntry.getOlatResource());
	}
	
	@Override
	public void preloadCache(Identity identity) {
		// 
	}

	@Override
	public void preloadCache(List<Identity> identities) {
		//
	}
}
