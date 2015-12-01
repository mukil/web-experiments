package de.akmiraketen.plugins.experiments;

import de.akmiraketen.plugins.experiments.model.ParticipantViewModel;
import de.akmiraketen.plugins.experiments.model.ScreenConfigViewModel;
import de.deepamehta.core.Association;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.service.Transactional;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;
import de.deepamehta.plugins.accesscontrol.model.ACLEntry;
import de.deepamehta.plugins.accesscontrol.model.AccessControlList;
import de.deepamehta.plugins.accesscontrol.model.Credentials;
import de.deepamehta.plugins.accesscontrol.model.Operation;
import de.deepamehta.plugins.accesscontrol.model.UserRole;
import de.deepamehta.plugins.accesscontrol.service.AccessControlService;
import de.deepamehta.plugins.files.DirectoryListing;
import de.deepamehta.plugins.files.DirectoryListing.FileItem;
import de.deepamehta.plugins.files.ItemKind;
import de.deepamehta.plugins.files.service.FilesService;
import de.deepamehta.plugins.workspaces.service.WorkspacesService;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import java.util.logging.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;



/**
 * A simple and flexible web-application to conduct experiments on the
 * perception and the processing of web-cartographies.
 *
 * @author Malte Reißig (<m_reissig@ifl-leipzig.de>), 2014-2015
 * @website https://github.com/mukil/web-experiments
 * @version 0.4-SNAPSHOT
 */
@Path("/experiment")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class WebExperimentsPlugin extends PluginActivator {

    private Logger log = Logger.getLogger(getClass().getName());

    private final String DEEPAMEHTA_VERSION = "DeepaMehta 4.4.3";
    private final String WEB_EXPERIMENTS_VERSION = "0.4-SNAPSHOT";
    private final String CHARSET = "UTF-8";

    // --- DeepaMehta 4 URIs
    
    private static final String USER_ACCOUNT_TYPE_URI = "dm4.accesscontrol.user_account";
    private static final String USERNAME_TYPE_URI = "dm4.accesscontrol.username";
    private static final String USER_PASSWORD_TYPE_URI = "dm4.accesscontrol.password";
    
    private static final String ROLE_PARENT = "dm4.core.child";
    private static final String ROLE_CHILD = "dm4.core.parent";
    private static final String ROLE_DEFAULT = "dm4.core.default";
    
    private static final String FILE_TYPE = "dm4.files.file";
    private static final String FILE_PATH_TYPE = "dm4.files.path";
    
    // --- Web Experiment URIs

    private static final String SCREEN_CONFIG_TYPE = "de.akmiraketen.screen_configuration";
    private static final String SCREEN_CONDITION_TYPE = "de.akmiraketen.screen_condition";

    // -- Per User Config

    private static final String SCREEN_SEEN_EDGE = "de.akmiraketen.screen_seen";
    private static final String ACTIVE_CONFIGURATION_EDGE = "de.akmiraketen.active_configuration";

    // -- Screen Report URIs

    private static final String SCREEN_REPORT_TYPE = "de.akmiraketen.screen_report";

    // -- Definitions

    private static final int OK_NR = 1;
    private static final int FAIL_NR = -1;

    // -- Settings

    private static final int NR_OF_USERS = 300;
    private static final String TEMPLATE_FOLDER = "web-experiments/templates";
    private static final String SYMBOL_FOLDER = "web-experiments/symbols";

    // --- Plugin Services
    
    @Inject
    private AccessControlService acService = null;
    
    @Inject
    private FilesService fileService = null;
    
    @Inject
    private WorkspacesService workspaceService = null;
    
    @Override
    public void init() {
        log.info("### Thank your for deploying Web Experiments " + WEB_EXPERIMENTS_VERSION);
    }
    
    @Override
    public void postInstall() {
        log.info(" ### Generating "+NR_OF_USERS+" user account for your web-experiment " + WEB_EXPERIMENTS_VERSION);
        generateNumberOfUsers();
        log.info(" ### Creating the \"/web-experiments/templates\" folder in your filerepo for screen templates " +
                "web-experiments " + WEB_EXPERIMENTS_VERSION);
        String parentFolderName = "web-experiments";
        createFolderWithName(parentFolderName, null);
        createFolderWithName("symbols", parentFolderName);
        createFolderWithName("templates", parentFolderName);
    }
    
    // --- All available routes to single pages
    
    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public InputStream getWelcomeScreen() {
        return getStaticResource("web/welcome.html");
    }
    
    @GET
    @Path("/icon")
    @Produces(MediaType.TEXT_HTML)
    public InputStream getMarkerView() {
        return getStaticResource("web/welcome.html");
    }
    
    @GET
    @Path("/finish")
    @Produces(MediaType.TEXT_HTML)
    public InputStream getFinishView() {
        return getStaticResource("web/welcome.html");
    }
    
    @GET    
    @Path("/trial/{trialId}/pinning")
    @Produces(MediaType.TEXT_HTML)
    public InputStream getTrialPinningScreen(@PathParam("trialId") String trialId) {
        return getStaticResource("web/pinning.html");
    }
    
    @GET
    @Path("/trial/{trialId}/estimation")
    @Produces(MediaType.TEXT_HTML)
    public InputStream getTrialEstimationScreen(@PathParam("trialId") String trialId) {
        return getStaticResource("web/estimation.html");
    }

    @GET
    @Path("/pract/{trialId}/pinning")
    @Produces(MediaType.TEXT_HTML)
    public InputStream getPracticeTrialPinningScreen(@PathParam("trialId") String trialId) {
        return getStaticResource("web/pinning.html");
    }

    @GET
    @Path("/pract/{trialId}/estimation")
    @Produces(MediaType.TEXT_HTML)
    public InputStream getPracticeTrialEstimationScreen(@PathParam("trialId") String trialId) {
        return getStaticResource("web/estimation.html");
    }

    @GET
    @Path("/intro/{trialId}")
    @Produces(MediaType.TEXT_HTML)
    public InputStream getIntroductionScreen(@PathParam("trialId") String trialId) {
        return getStaticResource("web/introduction.html");
    }

    @GET
    @Path("/pause/{trialId}")
    @Produces(MediaType.TEXT_HTML)
    public InputStream getPauseScreen() {
        return getStaticResource("web/pause.html");
    }

    @GET
    @Path("/start/{trialId}")
    @Produces(MediaType.TEXT_HTML)
    public InputStream getStartScreen() {
        return getStaticResource("web/start.html");
    }



    // --- REST Resources / API Endpoints
    
    /** 
     * 
     * @return  List of FileItems in JSON
     */
    @GET
    @Path("/symbol/all")
    @Transactional
    @Produces(MediaType.APPLICATION_JSON)
    public String getAllSymbolFileTopics() {
        DirectoryListing items = fileService.getDirectoryListing(SYMBOL_FOLDER);
        // ArrayList<Topic> symbols = new ArrayList<Topic>(); 
        ArrayList<JSONObject> symbolFiles = new ArrayList<JSONObject>();
        // 1) Gather svg-icon files from our symbols directory
        Iterator<FileItem> files = items.getFileItems().iterator();
        while (files.hasNext()) {
            FileItem fileItem = files.next();
            if (fileItem.getMediaType() == null && fileItem.getItemKind() != ItemKind.FILE) files.remove();
        }
        // 2) Create file topics (representing the files in our DB)
        Iterator<FileItem> icons = items.getFileItems().iterator();
        while (icons.hasNext()) {
            FileItem file = icons.next();
            Topic fileTopic = fileService.createFileTopic(file.getPath()); // fetches topic if existing
            JSONObject responseObject = new JSONObject();
            try {
                responseObject.put("path", file.getPath()).put("topic_id", fileTopic.getId());
            } catch (JSONException ex) {
                log.severe("Could not build up icon response list");
            }
            symbolFiles.add(responseObject);
        }
        return symbolFiles.toString();
    }

    
    /** 
     * Fetches all Screen Configurations currently loaded into the database.
     * @return 
     */
    
    @GET
    @Path("/screen/topics")
    public ResultList<RelatedTopic> getAllTrialConfigTopics() {
        return dms.getTopics(SCREEN_CONFIG_TYPE, 0).loadChildTopics();
    }

    @GET
    @Path("/screen/{screenTopicId}")
    public ScreenConfigViewModel getTrialConfigViewModel(@PathParam("screenTopicId") long id) {
        Topic trial = dms.getTopic(id);
        return new ScreenConfigViewModel(trial);
    }
    
    /** @GET
    @Path("/screen/config/import")
    @Transactional
    public Topic doImportUserTrialConfig() {
        return doImportUserTrialConfig(acService.getUsername());
    }

    @GET
    @Path("/screen/config/import/{username}")
    @Transactional
    public Topic doImportUserTrialConfig(@PathParam("username") String name) {
        try {
            // 1) fetch related file topic
            Topic username = acService.getUsername(name);
            Topic fileTopic = username.getRelatedTopic(ACTIVE_CONFIGURATION_EDGE, "dm4.core.default",
                    "dm4.core.default", "dm4.files.file");
            log.info("Loading Trial Config File Topic: " + fileTopic.getId()
                    + " fileName=" + fileTopic.getSimpleValue() + " for \"" + username);
            File trialConfig = fileService.getFile(fileTopic.getId());
            // 2) delete and create trial config topic
            ResultList<RelatedTopic> usersTrialConfigs = getUsersScreens(username);
            Iterator<RelatedTopic> i = usersTrialConfigs.iterator();
            while (i.hasNext()) {
                Topic topic = i.next();
                if (topic.getTypeUri().equals(SCREEN_CONFIG_TYPE)) {
                    log.fine(">>> Deleting former Trial Config Topic " + topic.getUri());
                    i.remove();
                    topic.delete();
                }
            }
            // 3) read in file topic's lines
            int nr = 1;
            BufferedReader br = new BufferedReader(new FileReader(trialConfig.getAbsolutePath()));
            try {
                String line = br.readLine();
                while (line != null) {
                    if (!line.startsWith("webexp.config")) {
                        log.fine("Line: " + line);
                        createNewTrialConfig(nr, line, username);
                        nr++;
                    }
                    line = br.readLine();
                }
            } finally {
                br.close();
            }
            return username;
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Could not import trial configuration fo user " + name, ex);
            return null;
        }
    } **/
    
    private ResultList<RelatedTopic> getUsersScreens(Topic username) {
        return username.getRelatedTopics(ACTIVE_CONFIGURATION_EDGE,
            "dm4.core.default", "dm4.core.default", SCREEN_CONFIG_TYPE, 0);
    }
    
    /** private void createNewTrialConfig(int lineNr, String config_line, Topic username) {
        // split csv-config file line
        String[] values = Pattern.compile("|", Pattern.LITERAL).split(config_line);
        String conditionUri = (values[3].trim().equals("Pinning")) ? TRIAL_CONDITION_A : ((values[3].trim().equals("No Pinning")) ? TRIAL_CONDITION_B : "");
        String trialMapId = values[2].trim();
        Topic map = dms.getTopic(TRIAL_CONFIG_MAP_ID, new SimpleValue(trialMapId));
        if (conditionUri.isEmpty()) log.severe("Trial Conditition URI could not be detected...!");
        if (map == null) log.severe("Map ID could not be detected...!");
        // build up topic model
        String configUri = "webexp.config." + username.getSimpleValue() + "_" + lineNr + "_" + values[0].trim();
        TopicModel trialConfig = new TopicModel(configUri, TRIAL_CONFIG_TYPE, new ChildTopicsModel()
                .put(TRIAL_CONFIG_NAME, values[1].trim())
                .putRef(TRIAL_CONFIG_MAP_ID, map.getId())
                .putRef(TRIAL_CONDITION_TYPE, conditionUri)
                .put(TRIAL_CONFIG_PLACE_TO_PIN, values[4].trim())
                .put(TRIAL_CONFIG_PLACE_FROM1, values[5].trim())
                .put(TRIAL_CONFIG_PLACE_TO1, values[6].trim())
                .put(TRIAL_CONFIG_PLACE_FROM2, values[7].trim())
                .put(TRIAL_CONFIG_PLACE_TO2, values[8].trim())
                .put(TRIAL_CONFIG_PLACE_FROM3, values[9].trim())
                .put(TRIAL_CONFIG_PLACE_TO3, values[10].trim())
                .put(TRIAL_CONFIG_PLACE_FROM4, values[11].trim())
                .put(TRIAL_CONFIG_PLACE_TO4, values[12].trim())
                .put(TRIAL_CONFIG_PLACE_FROM5, values[13].trim())
                .put(TRIAL_CONFIG_PLACE_TO5, values[14].trim())
                .put(TRIAL_CONFIG_MEMO_SEC, values[15].trim()));
        // create topic
        Topic trialConfigTopic = dms.createTopic(trialConfig);
        log.info(">>> Created new Trial Configuration: " + configUri + " (" + trialConfigTopic.getId() + ") for \"" + trialMapId + "\" (Topic: "+map.getId()+")");
        createTrialConfigUserAssignment(trialConfigTopic, username);
    } **/

    @GET
    @Path("/participant")
    public ParticipantViewModel getParticipantViewModel() {
        Topic username = getRequestingUsername();
        return new ParticipantViewModel(username, dms);
    }

    /**
     * Determines the next trial screen for a currently authenticated user and
     * routes the request to the respective multi-page (type).
     * TODO: Currently the page type is coded into the uri of each trial.
     */
    @GET
    @Path("/screen/next")
    public Response getNextScreen() throws URISyntaxException {
        // 1) get current username by http session (or throw a 401)
        Topic user = getRequestingUsername();
        // 2) get next trial config topic related to the user topic, discarding those related
        // to the user via a "trial_seen_edge".
        long trialId = getNextUnseenTrialId(user);
        // 2.1) If the particpant has seen all trials configured for her we redirect to our final screen.
        if (trialId == FAIL_NR) {
            log.info("Experiment finished, no configured trial left for requesting user");
            return Response.seeOther(new URI("/experiment/finish")).build();
        // 2.2) If there is yet an "unseen" trial configured for the user, we load and redirect
        // the request according to the "type".
        } else if (trialId != FAIL_NR){
            Topic trialConfig = dms.getTopic(trialId);
            ScreenConfigViewModel screenConfig = new ScreenConfigViewModel(trialConfig);
            log.info("Should REDIRECT to screen " + screenConfig.getScreenTemplateName());
            URI location = new URI("/experiment/screen/" + trialConfig.getId());
            return Response.seeOther(location).build();
        } else {
            return Response.ok(FAIL_NR).build(); // experiment finished > no unseen trial left
        }
    }

    @GET
    @Path("/screen/{trialId}/seen")
    @Transactional
    public Response doMarkScreenAsSeen(@PathParam("trialId") long trialId) {
        // 1) get current username by http session (or throw a 401)
        Topic user = getRequestingUsername();
        // ..
        Association trial_seen = user.getAssociation(SCREEN_SEEN_EDGE, ROLE_DEFAULT, ROLE_DEFAULT, trialId);
        if (trial_seen != null) {
            long nextTrialId = getNextUnseenTrialId(user);
            log.warning("This trial was already seen by our VP - Please load next => " + nextTrialId);
            return Response.ok(nextTrialId).build();
        }
        dms.createAssociation(new AssociationModel(SCREEN_SEEN_EDGE,
                new TopicRoleModel(user.getId(), "dm4.core.default"), 
                new TopicRoleModel(trialId, "dm4.core.default")));
        return Response.ok(OK_NR).build();
    }

    /**
     * An implementation to generate a CSV report containing all trial report (usage data) for all participants
     * which have completed some trials data as a response to a HTTP GET request.
     * @return String   The full contents of a CSV usage report.
     */
    @GET
    @Path("/report/generate")
    @Transactional
    @Produces(MediaType.TEXT_PLAIN)
    public String doGenerateCompleteReport() {
        // 1) check for logged in user (or throw a 401)
        getRequestingUsername();
        // 2) gather all reports from the db
        StringBuilder report = new StringBuilder();
        /** ResultList<RelatedTopic> propositi = dms.getTopics("dm4.accesscontrol.user_account", 0);
        report.append("VP ID\tTrial Condition\tMap ID\tTopin\tTopinname\tPinned\tPinRT\tPinInactive\t");
        report.append("Estfromname.1\tEsttoname.1\tEsttoscreen.1\tEstimation.1\tEststart.1\tEstend.1\tEstconfidence.1\t");
        report.append("Estfromname.2\tEsttoname.2\tEsttoscreen.2\tEstimation.2\tEststart.2\tEstend.2\tEstconfidence.2\t");
        report.append("Estfromname.3\tEsttoname.3\tEsttoscreen.3\tEstimation.3\tEststart.3\tEstend.3\tEstconfidence.3\t");
        report.append("Estfromname.4\tEsttoname.4\tEsttoscreen.4\tEstimation.4\tEststart.4\tEstend.4\tEstconfidence.4\t");
        report.append("Estfromname.5\tEsttoname.5\tEsttoscreen.5\tEstimation.5\tEststart.5\tEstend.5\tEstconfidence.5");
        report.append("\n");
        for (RelatedTopic vp : propositi.getItems()) {
            Topic username = vp.loadChildTopics(USERNAME_TYPE_URI).getChildTopics().getTopic(USERNAME_TYPE_URI);
            String vpId = username.getSimpleValue().toString();
            ResultList<RelatedTopic> trialReports = username.getRelatedTopics("dm4.core.association", "dm4.core.parent",
                    "dm4.core.child", TRIAL_REPORT_URI, 0);
            ArrayList<RelatedTopic> sortedTrialReports = getAllTrialReportsSortedByURI(trialReports);
            if (sortedTrialReports.size()> 0) {
                log.info("  Fetched " + sortedTrialReports.size() + " written to DB for " + vpId);
                for (RelatedTopic trialReport : sortedTrialReports) {
                    trialReport.loadChildTopics();
                    String trialConfigId = trialReport.getChildTopics().getString("de.akmiraketen.webexp.report_trial_config_id");
                    if (trialConfigId.contains("trial")) { //  exluding from report the practice || trialConfigId.contains("pract")
                        Topic trialConfig = dms.getTopic("uri", new SimpleValue(trialConfigId));
                        if (trialConfig != null) { // trial configuration could be loaded
                            // Collect General Info on Trial
                            String trialCondition = trialConfig.loadChildTopics(TRIAL_CONDITION_TYPE)
                                    .getChildTopics().getString(TRIAL_CONDITION_TYPE);
                            String mapId = trialConfig.loadChildTopics(TRIAL_CONFIG_MAP_ID)
                                    .getChildTopics().getString(TRIAL_CONFIG_MAP_ID);
                            // Collect Pinning Data
                            String placeToPinId = trialConfig.loadChildTopics(TRIAL_CONFIG_PLACE_TO_PIN)
                                .getChildTopics().getString(TRIAL_CONFIG_PLACE_TO_PIN);
                            Topic placeConfig = getConfiguredPlace(placeToPinId);
                            String placeToPinName = getConfiguredPlaceName(placeConfig);
                            String placeCoordinates = getConfiguredPlaceCoordinates(placeConfig);
                            String pinnedCoordinates = "-1;-1";
                            int pinningRT = -1, pinInactive = -1;
                            try {
                                pinnedCoordinates = trialReport.loadChildTopics(COORDINATES_PINNED_URI)
                                    .getChildTopics().getString(COORDINATES_PINNED_URI);
                                pinningRT = trialReport.loadChildTopics(REACTION_TIME_URI)
                                        .getChildTopics().getInt(REACTION_TIME_URI);
                                pinInactive = trialReport.loadChildTopics(COUNT_OUTSIDE_URI)
                                        .getChildTopics().getInt(COUNT_OUTSIDE_URI);
                            } catch (Exception e) {
                                log.warning("No pinning data was recorded / could be accessed for trial config: "
                                        + trialConfigId + " for " + username.getSimpleValue());
                            }
                            // Collect Estimation Report Data
                            String estimation1 = "", realToScreen1 = "", estFromName1 = "", estToName1 = "";
                            int estStart1 = -1, estEnd1 = -1, estConfidence1 = -1;
                            String estimation2 = "", realToScreen2 = "", estFromName2 = "", estToName2 = "";
                            int estStart2 = -1, estEnd2 = -1, estConfidence2 = -1;
                            String estimation3 = "", realToScreen3 = "", estFromName3 = "", estToName3 = "";
                            int estStart3 = -1, estEnd3 = -1, estConfidence3 = -1;
                            String estimation4 = "", realToScreen4 = "", estFromName4 = "", estToName4 = "";
                            int estStart4 = -1, estEnd4 = -1, estConfidence4 = -4;
                            String estimation5 = "", realToScreen5 = "", estFromName5 = "", estToName5 = "";
                            int estStart5 = -1, estEnd5 = -1, estConfidence5 = -1;
                            try {
                                List<Topic> estimationReports = trialReport.getChildTopics().getTopics(ESTIMATION_REPORT_URI);
                                for (Topic estimationReport : estimationReports) {
                                    int estimationNr = -1;
                                    estimationReport.loadChildTopics();
                                    estimationNr = estimationReport.getChildTopics().getInt(ESTIMATION_NR_URI);
                                    Topic fromPlace = getConfiguredPlace(estimationReport.getChildTopics().getString(ESTIMATION_FROM_PLACE_URI));
                                    Topic toPlace = getConfiguredPlace(estimationReport.getChildTopics().getString(ESTIMATION_TO_PLACE_URI));
                                    switch (estimationNr) {
                                        case 1:
                                            estFromName1 = getConfiguredPlaceName(fromPlace);
                                            estToName1 = getConfiguredPlaceName(toPlace);
                                            realToScreen1 = estimationReport.getChildTopics().getString(REAL_SCREEN_COORDINATES_URI);
                                            estimation1 = estimationReport.getChildTopics().getString(ESTIMATED_SCREEN_COORDINATES_URI);
                                            estStart1 = estimationReport.getChildTopics().getInt(ESTIMATION_TO_START_TIME_URI);
                                            estEnd1 = estimationReport.getChildTopics().getInt(ESTIMATION_TIME_URI);
                                            estConfidence1 = estimationReport.getChildTopics().getInt(ESTIMATION_CONFIDENCE);
                                            break;
                                        case 2:
                                            estFromName2 = getConfiguredPlaceName(fromPlace);
                                            estToName2 = getConfiguredPlaceName(toPlace);
                                            realToScreen2 = estimationReport.getChildTopics().getString(REAL_SCREEN_COORDINATES_URI);
                                            estimation2 = estimationReport.getChildTopics().getString(ESTIMATED_SCREEN_COORDINATES_URI);
                                            estStart2 = estimationReport.getChildTopics().getInt(ESTIMATION_TO_START_TIME_URI);
                                            estEnd2 = estimationReport.getChildTopics().getInt(ESTIMATION_TIME_URI);
                                            estConfidence2 = estimationReport.getChildTopics().getInt(ESTIMATION_CONFIDENCE);
                                            break;
                                        case 3:
                                            estFromName3 = getConfiguredPlaceName(fromPlace);
                                            estToName3 = getConfiguredPlaceName(toPlace);
                                            realToScreen3 = estimationReport.getChildTopics().getString(REAL_SCREEN_COORDINATES_URI);
                                            estimation3 = estimationReport.getChildTopics().getString(ESTIMATED_SCREEN_COORDINATES_URI);
                                            estStart3 = estimationReport.getChildTopics().getInt(ESTIMATION_TO_START_TIME_URI);
                                            estEnd3 = estimationReport.getChildTopics().getInt(ESTIMATION_TIME_URI);
                                            estConfidence3 = estimationReport.getChildTopics().getInt(ESTIMATION_CONFIDENCE);
                                            break;
                                        case 4:
                                            estFromName4 = getConfiguredPlaceName(fromPlace);
                                            estToName4 = getConfiguredPlaceName(toPlace);
                                            realToScreen4 = estimationReport.getChildTopics().getString(REAL_SCREEN_COORDINATES_URI);
                                            estimation4 = estimationReport.getChildTopics().getString(ESTIMATED_SCREEN_COORDINATES_URI);
                                            estStart4 = estimationReport.getChildTopics().getInt(ESTIMATION_TO_START_TIME_URI);
                                            estEnd4 = estimationReport.getChildTopics().getInt(ESTIMATION_TIME_URI);
                                            estConfidence4 = estimationReport.getChildTopics().getInt(ESTIMATION_CONFIDENCE);
                                            break;
                                        case 5:
                                            estFromName5 = getConfiguredPlaceName(fromPlace);
                                            estToName5 = getConfiguredPlaceName(toPlace);
                                            realToScreen5 = estimationReport.getChildTopics().getString(REAL_SCREEN_COORDINATES_URI);
                                            estimation5 = estimationReport.getChildTopics().getString(ESTIMATED_SCREEN_COORDINATES_URI);
                                            estStart5 = estimationReport.getChildTopics().getInt(ESTIMATION_TO_START_TIME_URI);
                                            estEnd5 = estimationReport.getChildTopics().getInt(ESTIMATION_TIME_URI);
                                            estConfidence5 = estimationReport.getChildTopics().getInt(ESTIMATION_CONFIDENCE);
                                            break;
                                    }
                                }
                            } catch (Exception e) {
                                log.warning("No estimation data was recorded during trial " + trialConfigId + " for " + username.getSimpleValue());
                            }
                            // Write line
                            report.append(vpId + "\t" + trialCondition + "\t" + mapId + "\t" + placeCoordinates + "\t" + placeToPinName
                                    + "\t" + pinnedCoordinates + "\t" + pinningRT + "\t" + pinInactive
                                    + "\t" + estFromName1 + "\t" + estToName1 + "\t" + realToScreen1 + "\t" + estimation1 + "\t" + estStart1 + "\t" + estEnd1 + "\t" + estConfidence1
                                    + "\t" + estFromName2 + "\t" + estToName2 + "\t" + realToScreen2 + "\t" + estimation2 + "\t" + estStart2 + "\t" + estEnd2 + "\t" + estConfidence2
                                    + "\t" + estFromName3 + "\t" + estToName3 + "\t" + realToScreen3 + "\t" + estimation3 + "\t" + estStart3 + "\t" + estEnd3 + "\t" + estConfidence3
                                    + "\t" + estFromName4 + "\t" + estToName4 + "\t" + realToScreen4 + "\t" + estimation4 + "\t" + estStart4 + "\t" + estEnd4 + "\t" + estConfidence4
                                    + "\t" + estFromName5 + "\t" + estToName5 + "\t" + realToScreen5 + "\t" + estimation5 + "\t" + estStart5 + "\t" + estEnd5 + "\t" + estConfidence5);
                            report.append("\n");
                        } else { // trial configuration could not be loaded..
                            log.warning("System Trial Configuration changed"
                                + " - Fetching Trial Config with URI: " + trialConfigId + " failed --- SKIPPED");
                        }
                    }
                }
            }
        } **/
        return report.toString();
    }


    
    // --- Helper Methods
    
    private ArrayList<RelatedTopic> getScreenTopicsSortedByURI(ResultList<RelatedTopic> all) {
        // build up sortable collection of all result-items
        ArrayList<RelatedTopic> in_memory = new ArrayList<RelatedTopic>();
        for (RelatedTopic obj : all) {
            in_memory.add(obj);
        }
        // sort all result-items
        Collections.sort(in_memory, new Comparator<RelatedTopic>() {
            public int compare(RelatedTopic t1, RelatedTopic t2) {
                try { // ### webexp.config.intro + webexp.config.pract
                    if (!t1.getUri().contains(".") && !t2.getUri().contains(".")) {
                        String one = t1.getUri().substring(t1.getUri().lastIndexOf(".") + 1);
                        String two = t2.getUri().substring(t2.getUri().lastIndexOf(".") + 1);
                        if ( Long.parseLong(one) < Long.parseLong(two)) return -1;
                        if ( Long.parseLong(one) > Long.parseLong(two)) return 1;
                    }
                } catch (Exception nfe) {
                    log.warning("Error while accessing URI of Topic 1: " + t1.getUri() + " Topic2: "
                            + t2.getUri() + " nfe: " + nfe.getMessage());
                    return 0;
                }
                return 0;
            }
        });
        return in_memory;
    }
    
    /** private ArrayList<RelatedTopic> getAllTrialReportsSortedByURI(ResultList<RelatedTopic> all) {
        // build up sortable collection of all result-items
        ArrayList<RelatedTopic> in_memory = new ArrayList<RelatedTopic>();
        for (RelatedTopic obj : all) {
            in_memory.add(obj);
        }
        // sort all result-items
        Collections.sort(in_memory, new Comparator<RelatedTopic>() {
            public int compare(RelatedTopic t1, RelatedTopic t2) {
                try {
                    t1.loadChildTopics("de.akmiraketen.screen_report_config_id");
                    t2.loadChildTopics("de.akmiraketen.webexp.report_trial_config_id");
                    String trialConfigIdOne = t1.getChildTopics().getString("de.akmiraketen.webexp.report_trial_config_id");
                    String trialConfigIdTwo = t2.getChildTopics().getString("de.akmiraketen.webexp.report_trial_config_id");
                    if (trialConfigIdOne.contains("trial") && trialConfigIdTwo.contains("trial")) {
                        String one = trialConfigIdOne.substring(trialConfigIdOne.lastIndexOf("_") + 6);
                        String two = trialConfigIdTwo.substring(trialConfigIdTwo.lastIndexOf("_") + 6);
                        if ( Long.parseLong(one) < Long.parseLong(two)) return -1;
                        if ( Long.parseLong(one) > Long.parseLong(two)) return 1;
                    }
                } catch (Exception nfe) {
                    log.warning("Error while accessing URI of Topic 1: " + t1.getUri() + " Topic2: "
                            + t2.getUri() + " nfe: " + nfe.getMessage());
                    return 1;
                }
                return 1;
            }
        });
        return in_memory;
    } **/

    /**
     * Iterates all trial configurations related to a specific user and gets the topic id of the first one
     * (those are ordered by an ordinal number in their respective URI) without an association of type
     * "de.akmiraketen.webexp.trial_seen_edge".
     */
    private long getNextUnseenTrialId(Topic username) {
        ResultList<RelatedTopic> all_trials = getUsersScreens(username);
        ArrayList<RelatedTopic> sorted_trial_config_lines = getScreenTopicsSortedByURI(all_trials);
        Iterator<RelatedTopic> iterator = sorted_trial_config_lines.iterator();
        while (iterator.hasNext()) {
            RelatedTopic trialConfig = iterator.next();
            if (!hasSeenTrial(username, trialConfig)) {
                return trialConfig.getId();
            }
        }
        return FAIL_NR; // experiment finished > no unseen trial left
    }

    /**
     * Checks if a trial config topic related to the given user has an association of type
     * "de.akmiraketen.webexp.trial_seen_edge".
     * NOTE: This type of association must be manually created by the respective page-type (js, frontend developer).
     */
    private boolean hasSeenTrial(Topic user, Topic trial) {
        Association trial_seen = trial.getAssociation(SCREEN_SEEN_EDGE, ROLE_DEFAULT, ROLE_DEFAULT, user.getId());
        return trial_seen != null;
    }

    private Topic getOrCreateTrialPinningReportTopic (long trialId, Topic user) {
        DeepaMehtaTransaction tx = dms.beginTx();
        Topic report = null;
        String trialConfigUri = dms.getTopic(trialId).getUri();
        ResultList<RelatedTopic> trialReports = user.getRelatedTopics("dm4.core.association", 
                "dm4.core.parent", "dm4.core.child", "de.akmiraketen.webexp.trial_report", 0);
        for (RelatedTopic trialReport : trialReports) {
            String trial = trialReport.getChildTopics().getString("de.akmiraketen.webexp.report_trial_config_id");
            if (trialConfigUri.equals(trial)) {
                log.info("Re-using Trial Report for Trial: " + trialConfigUri + " and VP " + user.getSimpleValue());
                return trialReport;
            }
        }
        try {
            log.info("Creating new Trial Report for user " + user.getSimpleValue() + " and Trial " + trialConfigUri);
            ChildTopicsModel child = new ChildTopicsModel(new JSONObject()
                    .put("de.akmiraketen.webexp.report_trial_config_id", trialConfigUri));
            TopicModel model = new TopicModel("de.akmiraketen.webexp.trial_report", child);
            report = dms.createTopic(model);
            dms.createAssociation(new AssociationModel("dm4.core.association", 
                    new TopicRoleModel(user.getId(), "dm4.core.parent"), 
                    new TopicRoleModel(report.getId(), "dm4.core.child")));
            tx.success();
        } catch (JSONException ex) {
            log.severe("Could not create a Trial Report for user ..");
        } finally {
            tx.finish();
        }
        return report;
    }

    private Topic getRequestingUsername() {
        String username = acService.getUsername();
        if (username == null || username.isEmpty()) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        return dms.getTopic(USERNAME_TYPE_URI, new SimpleValue(username));
    }
    
    private void generateNumberOfUsers() {
        // for 1000 do acService.createUser()
        log.info("### Setting up new users for Web Experiments");
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            for (int i=1; i<=NR_OF_USERS; i++) {
                String username = "VP "+ i;
                if (isUsernameAvailable(username)) {
                    Credentials cred = new Credentials(username, "");
                    ChildTopicsModel userAccount = new ChildTopicsModel()
                        .put(USERNAME_TYPE_URI, cred.username)
                        .put(USER_PASSWORD_TYPE_URI, cred.password);
                    TopicModel userModel = new TopicModel(USER_ACCOUNT_TYPE_URI, userAccount);
                    Topic vpAccount = dms.createTopic(userModel);
                    Topic usernameTopic = vpAccount.loadChildTopics(USERNAME_TYPE_URI)
                            .getChildTopics().getTopic(USERNAME_TYPE_URI);
                    if (usernameTopic != null && workspaceService.getDefaultWorkspace() != null) {
                        workspaceService.assignToWorkspace(usernameTopic, workspaceService.getDefaultWorkspace().getId());
                        setDefaultAdminACLEntries(vpAccount);
                        log.info("Created user \"" + username + "\" for web-experiments.");
                    } else {
                        log.info("Could not create new user, topic username: " + username+ ", workspace:" +
                                workspaceService.getDefaultWorkspace());
                    }
                } else {
                    log.info("DEBUG: Username is already taken ..");
                }
            }
            tx.success();
        } finally {
            tx.finish();
        }
    }
    
    private DeepaMehtaObject setDefaultAdminACLEntries(DeepaMehtaObject item) {
        // Let's repair broken/missing ACL-Entries
        ACLEntry writeEntry = new ACLEntry(Operation.WRITE, UserRole.CREATOR, UserRole.OWNER);
        acService.setACL(item, new AccessControlList(writeEntry));
        acService.setCreator(item, "admin");
        acService.setOwner(item, "admin");
        return item;
    }
    
    private void createFolderWithName(String folderName, String parentFolderName) {
        DeepaMehtaTransaction tx = dms.beginTx();
        String parent = "/";
        if (parentFolderName != null) parent = parentFolderName;
        try {
            File item = fileService.getFile(parent + "/" + folderName); // throws RuntimeException if no result
            if (!item.isDirectory()) { // folder does not exist
                fileService.createFolder(folderName, parent);
            } else  {
                log.info("OK - Folder already exists");
            }
            tx.success();
        } catch (RuntimeException fe) { // file or folder does not exist
            log.info("Cause: " + fe.getCause().toString());
            if (fe.getCause().toString().contains("does not exist")) {
                try {
                    log.info("Folder does not exist!..");
                    fileService.createFolder(folderName, parent); // might throw permission denied error
                    log.info("CREATED new Folder " + folderName);
                    tx.success();
                } catch (RuntimeException re) {
                    log.severe("Most probably DeepaMehta cant write to your filerepo, "
                            + "please check the filrepo configuration in the pom.xml");
                    throw new RuntimeException(re);
                }
            } else {
                // throw new RuntimeException(fe);
                log.info("OK - Folder already exists");
            }
        } finally {
            tx.finish();
        }
    }
    
    private boolean isUsernameAvailable(String username) {
        Topic userName = dms.getTopic(USERNAME_TYPE_URI, new SimpleValue(username));
        return (userName == null);
    }
    
    private Association createTrialConfigUserAssignment(Topic trialConfig, Topic username) {
        return dms.createAssociation(new AssociationModel(ACTIVE_CONFIGURATION_EDGE,
            new TopicRoleModel(username.getId(), ROLE_DEFAULT),
            new TopicRoleModel(trialConfig.getId(), ROLE_DEFAULT)));
    }

}
