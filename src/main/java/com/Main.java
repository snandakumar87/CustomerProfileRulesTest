package com;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kie.api.KieServices;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.ClassObjectFilter;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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
        ObjectMapper mapper = new ObjectMapper();

        ReleaseId rId1 = KIE_SERVICES.newReleaseId("com.redhat", "Personalization_Rules", "1.0.0");
        kieContainer = KIE_SERVICES.newKieContainer(rId1);


        Main main = new Main();
        //Test Data setup
        CustomerProfile customerProfile = main.getCustomerProfile();
        // Process the incoming transaction.
        List<eventAnalysis> analysisModels = main.processTransactionForAllEvents(customerProfile.getEvents(),customerProfile.getModels());
        customerProfile.setEventAnalysisModels(analysisModels);
        try {
            System.out.print(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(customerProfile));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }


    }

    private  CustomerProfile getCustomerProfile() {
        ObjectMapper mapper = new ObjectMapper();

        CustomerProfile testObj = null;
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try {
        File file = new File(classLoader.getResource("CustomerProfile.json").getFile());
        InputStream is = new FileInputStream(file);
        testObj = mapper.readValue(is, CustomerProfile.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return testObj;
    }

    private   List<eventAnalysis> processTransactionForAllEvents(List<Event> events, List<TrainingModel> models) {
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

    private  eventAnalysis processTransaction(Event events, TrainingModel trainingModel) {


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
