package com;


import org.kie.api.KieServices;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.ClassObjectFilter;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd:HHmmssSSS");

    private static final KieServices KIE_SERVICES = KieServices.Factory.get();

    private static KieContainer kieContainer;

    public static void main(String[] args) {


        ReleaseId rId1 = KIE_SERVICES.newReleaseId("com.redhat", "Personalization_Rules", "1.0.0");
        kieContainer = KIE_SERVICES.newKieContainer(rId1);



        //Test Data setup
        CustomerProfile customerProfile = getCustomerProfile();
        // Process the incoming transaction.
        List<eventAnalysis> analysisModels = processTransactionForAllEvents(customerProfile.getEvents(),customerProfile.getModels());
        customerProfile.setEventAnalysisModels(analysisModels);
        System.out.print(customerProfile);


    }

    private static CustomerProfile getCustomerProfile() {
        CustomerProfile customerProfile = new CustomerProfile();

        customerProfile.setCustomerAccountNumber("TEST_ACCOUNT_NUMBER");
        customerProfile.setCustomerName("TEST_CUSTOMER");
        Event event = new Event("CC_BALANCE_PAYMENT","ONLINE",new Date(),"GFFHJU-7657","LATE_PAYMENT");
        Event event1 = new Event("CC_BALANCE_PAYMENT","ONLINE",new Date(),"GFFHJU-7657","MIN_DUE");
        Event event2 = new Event("DISPUTE","IVR",new Date(),"GFFHJU-7657","CASE_CREATED");
        Event event3 = new Event("DISPUTE","IVR",new Date(),"GFFHJU-7657","CASE_CLOSED");
        Event event4 = new Event("ONLINE","IVR",new Date(),"GFFHJU-7657","CASE_CREATED");
        List<Event> eventsForProcessing = new LinkedList<Event>();
        eventsForProcessing.add(event);
        eventsForProcessing.add(event1);
        eventsForProcessing.add(event2);
        eventsForProcessing.add(event3);
        eventsForProcessing.add(event4);

        List<TrainingModel> trainingModels = new LinkedList<TrainingModel>();
        TrainingModel trainingModel = new TrainingModel("CUSTOMER_GOOD_STANDING",100);
        TrainingModel trainingModel1 = new TrainingModel("HIGH_BALANCE_DEBT",100);
        trainingModels.add(trainingModel);
        trainingModels.add(trainingModel1);

        customerProfile.setEvents(eventsForProcessing);
        customerProfile.setModels(trainingModels);
        return customerProfile;
    }

    private  static List<eventAnalysis> processTransactionForAllEvents(List<Event> events, List<TrainingModel> models) {
        List<eventAnalysis> eventAnalysisModel = new LinkedList<eventAnalysis>();
        for(Event event:events) {
            for (TrainingModel trainingModel:models) {
                eventAnalysis analysis = processTransaction(event,trainingModel);
                if(null != analysis) {
                    eventAnalysisModel.add(analysis);
                }
            }
        }
        return eventAnalysisModel;

    }

    private static eventAnalysis processTransaction(Event events, TrainingModel trainingModel) {


        KieSession kieSession = kieContainer.newKieSession();

        kieSession.insert(events);

        kieSession.insert(trainingModel);

        // And fire the rules.
        kieSession.fireAllRules();
        eventAnalysis eventAnalys = null;

        Collection<?> objects = kieSession.getObjects(new ClassObjectFilter(eventAnalysis.class));

        eventAnalys = (eventAnalysis) objects.iterator().next();

        // Dispose the session to free up the resources.
        kieSession.dispose();
        return eventAnalys;
    }




}
