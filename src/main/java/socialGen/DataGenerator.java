package socialGen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import conf.PartitionConfiguration;
import datatype.Date;
import datatype.DateTime;
import datatype.Message;
import datatype.Point;
import entity.Employment;
import entity.FacebookMessage;
import entity.FacebookUser;
import entity.TweetMessage;
import entity.TwitterUser;
import generator.RandomDateGenerator;
import generator.RandomEmploymentGenerator;
import generator.RandomIdSelector;
import generator.RandomLocationGenerator;
import generator.RandomMessageGenerator;
import generator.RandomNameGenerator;
import utility.FileAppender;
import utility.FileUtil;

public class DataGenerator {

    private final static int MAX_DIGIT = 15;
    private final static int EMPLOYMENT_LAST_START_YEAR = 2012;
    private final static int EMPLOYED_USERS_RATIO = 60;
    private final static int START_YEAR = 2000;
    private final static int START_MONTH = 1;
    private final static int START_DAY = 1;
    private final static int END_YEAR = 2014;
    private final static int END_MONTH = 8;
    private final static int END_DAY = 30;
    private final static int START_LATITUDE = 24;
    private final static int END_LATITUDE = 49;
    private final static int START_LONGITUDE = 66;
    private final static int END_LONGITUDE = 98;
    private final static int MAX_FRIENDS = 400;
    private final static int MAX_STATUS_COUNT = 500;

    private static RandomDateGenerator randDateGen;
    private static RandomNameGenerator randNameGen;
    private static RandomEmploymentGenerator randEmpGen;
    private static RandomMessageGenerator randMessageGen;
    private static RandomLocationGenerator randLocationGen;
    private static long numOfFBUsers;
    private static long numOfTWUsers;
    private static int avgMsgPerFBU;
    private static int avgTweetPerTWU;
    private static long fbUserId;
    private static long fbMessageId;
    private static long twMessageId;

    private static Random random = new Random(System.currentTimeMillis());
    private static String outputDir;
    private static PartitionConfiguration partition;

    private static FacebookUser fbUser = new FacebookUser();
    private static TwitterUser twUser = new TwitterUser();
    private static FacebookMessage fbMessage = new FacebookMessage();
    private static TweetMessage twMessage = new TweetMessage();

    private static void generateFacebookUsers(long numFacebookUsers) throws IOException {
        FileAppender appender = FileUtil.getFileAppender(outputDir + "/" + "fb_users.adm", true, true);
        FileAppender messageAppender = FileUtil.getFileAppender(outputDir + "/" + "fb_message.adm", true, true);
        for (int i = 0; i < numFacebookUsers; i++) {
            getFacebookUser(null);
            appender.appendToFile(fbUser.toString());
            int numOfMsg = random.nextInt(2 * avgMsgPerFBU + 1);
            generateFacebookMessages(fbUser, messageAppender, numOfMsg);
        }
        appender.close();
        messageAppender.close();
    }

    private static void generateTwitterUsers(long numTwitterUsers) throws IOException {
        FileAppender messageAppender = FileUtil.getFileAppender(outputDir + "/" + "tw_message.adm", true, true);
        for (int i = 0; i < numTwitterUsers; i++) {
            getTwitterUser(null);
            int numOfTweets = random.nextInt(2 * avgTweetPerTWU + 1);
            generateTwitterMessages(twUser, messageAppender, numOfTweets);
        }
        messageAppender.close();
    }

    private static void generateFacebookMessages(FacebookUser user, FileAppender appender, int numMsg)
            throws IOException {
        Message message;
        for (int i = 0; i < numMsg; i++) {
            message = randMessageGen.getNextRandomMessage(false);
            Point location = randLocationGen.getRandomPoint();
            DateTime sendTime = randDateGen.getNextRandomDatetime();
            fbMessage.reset(fbMessageId++, user.getId(), generateRandomLong(1, (numOfFBUsers * avgMsgPerFBU)), location,
                    sendTime, message);
            appender.appendToFile(fbMessage.toString());
        }
    }

    private static void generateTwitterMessages(TwitterUser user, FileAppender appender, long numMsg)
            throws IOException {
        Message message;
        for (int i = 0; i < numMsg; i++) {
            message = randMessageGen.getNextRandomMessage(true);
            Point location = randLocationGen.getRandomPoint();
            DateTime sendTime = randDateGen.getNextRandomDatetime();
            twMessage.reset(twMessageId, user, location, sendTime, message.getReferredTopics(), message);
            twMessageId++;
            appender.appendToFile(twMessage.toString());
        }
    }

    public static void main(String args[]) throws Exception {
        String controllerInstallDir = null;
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        } else {
            controllerInstallDir = args[0];
            String partitionConfXML = controllerInstallDir + "/output/partition-conf.xml";
            String partitionName = args[1];
            partition = XMLUtil.getPartitionConfiguration(partitionConfXML, partitionName);
        }

        randDateGen = new RandomDateGenerator(new Date(START_MONTH, START_DAY, START_YEAR),
                new Date(END_MONTH, END_DAY, END_YEAR));

        String firstNameFile = controllerInstallDir + "/metadata/firstNames.txt";
        String lastNameFile = controllerInstallDir + "/metadata/lastNames.txt";
        String msgGenConfigFile = controllerInstallDir + "/metadata/config.txt";
        String orgList = controllerInstallDir + "/metadata/org_list.txt";

        randNameGen = new RandomNameGenerator(firstNameFile, lastNameFile);
        randEmpGen = new RandomEmploymentGenerator(EMPLOYED_USERS_RATIO, new Date(START_MONTH, START_DAY, START_YEAR),
                new Date(END_MONTH, END_DAY, EMPLOYMENT_LAST_START_YEAR), orgList);
        randLocationGen = new RandomLocationGenerator(START_LATITUDE, END_LATITUDE, START_LONGITUDE, END_LONGITUDE);
        String parentDir = controllerInstallDir + "/metadata";
        randMessageGen = new RandomMessageGenerator(msgGenConfigFile, parentDir);
        numOfFBUsers = (partition.getTargetPartition().getFbUserKeyMax()
                - partition.getTargetPartition().getFbUserKeyMin()) + 1;
        numOfTWUsers = (partition.getTargetPartition().getTwUserKeyMax()
                - partition.getTargetPartition().getTwUserKeyMin()) + 1;
        avgMsgPerFBU = partition.getTargetPartition().getAvgMsgPerFBU();
        avgTweetPerTWU = partition.getTargetPartition().getAvgTweetPerTWU();
        fbUserId = partition.getTargetPartition().getFbUserKeyMin();
        fbMessageId = partition.getTargetPartition().getFbMessageIdMin();
        twMessageId = partition.getTargetPartition().getTwMessageIdMin();
        outputDir = partition.getSourcePartition().getPath();

        generateData();
    }

    private static void printUsage() {
        System.out.println(" Error: Invalid number of arguments ");
        System.out.println(" Usage :" + " DataGenerator <path to configuration file> <partition name> ");
    }

    private static void generateData() throws IOException {
        generateFacebookUsers(numOfFBUsers);
        generateTwitterUsers(numOfTWUsers);
        System.out.println("\nData generation in partition :" + partition.getTargetPartition().getName() + " finished");
    }

    private static void getFacebookUser(String usernameSuffix) {
        String suggestedName = randNameGen.getRandomName();
        String[] nameComponents = suggestedName.split(" ");
        String name = nameComponents[0] + " " + nameComponents[1];
        if (usernameSuffix != null) {
            name = name + usernameSuffix;
        }
        long id = fbUserId++;
        String alias = getUniqueAlias(nameComponents[0], id, MAX_DIGIT);
        String userSince = randDateGen.getNextRandomDatetime().toString();
        int numFriends = random.nextInt(11);
        long[] friendIds = RandomIdSelector.getKFromN(numFriends, (numOfFBUsers));
        int empCount = 1 + random.nextInt(3);
        ArrayList<Employment> emp = new ArrayList<Employment>(empCount);
        for (int i = 0; i < empCount; i++) {
            Employment e = randEmpGen.getRandomEmployment();
            emp.add(new Employment(e.getOrganization(), e.getStartDate(), e.getEndDate()));
        }
        fbUser.reset(id, alias, name, userSince, friendIds, emp);
    }

    private static void getTwitterUser(String usernameSuffix) {
        String suggestedName = randNameGen.getRandomName();
        String[] nameComponents = suggestedName.split(" ");
        String screenName = nameComponents[0] + nameComponents[1] + randNameGen.getRandomNameSuffix();
        String name = suggestedName;
        if (usernameSuffix != null) {
            name = name + usernameSuffix;
        }
        int numFriends = random.nextInt(MAX_FRIENDS);
        int statusesCount = random.nextInt(MAX_STATUS_COUNT);
        int followersCount = (numFriends - 5) + random.nextInt(200);
        twUser.reset(screenName, numFriends, statusesCount, name, followersCount);
    }

    private static long generateRandomLong(long x, long y) {
        return (x + ((long) (random.nextDouble() * (y - x))));
    }

    private static String getUniqueAlias(String name, long id, int maxDigit) {
        int digits = maxDigit;
        if (id != 0) {
            digits -= ((int) (Math.log10(id) + 1));
        }
        String pad = "";
        for (int i = 0; i < digits; i++) {
            pad = "0" + pad;
        }
        return name + pad + id;
    }
}