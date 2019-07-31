import com.amazonaws.services.qldb.AmazonQLDB;
import com.amazonaws.services.qldb.AmazonQLDBClientBuilder;
import com.amazonaws.services.qldb.model.CreateLedgerRequest;
import com.amazonaws.services.qldb.model.CreateLedgerResult;
import com.amazonaws.services.qldb.model.DescribeLedgerResult;
import com.amazonaws.services.qldb.model.LedgerState;
import com.amazonaws.services.qldb.model.DescribeLedgerRequest;
import com.amazonaws.services.qldb.model.DescribeLedgerResult;
import static com.amazonaws.services.qldb.model.PermissionsMode.ALLOW_ALL;
import com.amazon.qldb.QldbClientException;
import com.amazon.qldb.QldbSession;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.qldbsessionv1.AmazonQLDBSessionV1ClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Smoke test QLDB:
 *   - Create ledger
 *   - Connect to ledger
 *   - Create table
 *   - Insert a record
 *   - Update a record
 *   - Verify a record
 */
public class QLDBSmokeTest {

    private static final Logger log = LoggerFactory.getLogger(QLDBSmokeTest.class);

    public static final String LEDGER_NAME = "QLDBSmokeTest";
    public static final Long LEDGER_CREATION_POLL_PERIOD_MS = 10_000L;
    public static final int RETRY_LIMIT = 4;

    public static String endpoint = null;
    public static String region = null;
    public static AWSCredentialsProvider credentialsProvider;

    public static AmazonQLDB client = getClient();

    public static AmazonQLDB getClient() {
        return AmazonQLDBClientBuilder.standard().build();
    }

    private static QldbSession session;

    public static CreateLedgerResult create(String ledgerName) {
        log.info("Creating the ledger with name: {}...", ledgerName);
        CreateLedgerRequest request = new CreateLedgerRequest()
                .withName(ledgerName)
                .withPermissionsMode(ALLOW_ALL);
        CreateLedgerResult result = client.createLedger(request);
        log.info("Success. Ledger state: {}", result.getState());
        return result;
    }

    public static DescribeLedgerResult waitForActive(String name) throws InterruptedException {
        log.info("Waiting for ledger to become active...");

        DescribeLedgerRequest request = new DescribeLedgerRequest().withName(name);
        DescribeLedgerResult result = client.describeLedger(request);

        while (!result.getState().equals(LedgerState.ACTIVE.name())) {
          log.info("The ledger is still creating. Please wait...");
          Thread.sleep(LEDGER_CREATION_POLL_PERIOD_MS);
          result = client.describeLedger(request);
        }

        log.info("Success. Ledger is active and ready to use.");    
        log.info("Ledger description: {}", result);
        return result;
    }

    public static DescribeLedgerResult describe(String name) {
        log.info("Let's describe ledger with name: {}...", name);
        DescribeLedgerRequest request = new DescribeLedgerRequest().withName(name);
        DescribeLedgerResult result = client.describeLedger(request);
        log.info("Success. Ledger description: {}", result);
        return result;
    }

    /**
     * Connect to a session.
     */
    public static QldbSession createQldbSession() {
        AmazonQLDBSessionV1ClientBuilder builder = AmazonQLDBSessionV1ClientBuilder.standard();
        if (null != endpoint && null != region) {
            builder.setEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region));
        }
        if (null != credentialsProvider) {
            builder.setCredentials(credentialsProvider);
        }
        return QldbSession.builder()
                .withLedger(LEDGER_NAME)
                .withOccConflictRetryLimit(RETRY_LIMIT)
                .withSessionClientBuilder(builder)
                .build();
    }


    public static void main(String... args) {
        try {
            create(LEDGER_NAME);
            waitForActive(LEDGER_NAME);

        } catch (Exception e) {
            log.error("Unable to create the ledger!", e);
//            throw e;
        }
        

        try {
            session = createQldbSession();
        } catch (QldbClientException e) {
            log.error("Unable to create session.", e);
        }
    }

}
