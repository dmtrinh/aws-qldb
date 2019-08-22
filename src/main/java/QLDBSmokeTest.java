import com.amazonaws.services.qldb.AmazonQLDB;
import com.amazonaws.services.qldb.AmazonQLDBClientBuilder;
import com.amazonaws.services.qldb.model.CreateLedgerRequest;
import com.amazonaws.services.qldb.model.CreateLedgerResult;
import com.amazonaws.services.qldb.model.DescribeLedgerResult;
import com.amazonaws.services.qldb.model.LedgerState;
import com.amazonaws.services.qldb.model.DescribeLedgerRequest;
import com.amazonaws.services.qldb.model.DescribeLedgerResult;
import static com.amazonaws.services.qldb.model.PermissionsMode.ALLOW_ALL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.amazon.ion.IonReader;
import com.amazon.ion.IonValue;
import com.amazon.ion.system.IonReaderBuilder;
import com.amazon.qldb.QldbClientException;
import com.amazon.qldb.QldbSession;
import com.amazon.qldb.transaction.TransactionExecutor;
import com.amazon.qldb.transaction.result.Result;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.qldbsessionv1.AmazonQLDBSessionV1ClientBuilder;
import com.fasterxml.jackson.dataformat.ion.IonObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Smoke test QLDB:
 *   + Create a ledger
 *   + Connect to the ledger
 *   + Create some tables
 *   - Insert some data
 *   - Update some data
 *   - Retrieve modification history
 *   - Verify document revision has is valid
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

    public static int createTable(TransactionExecutor txn, String tableName) {
        log.info("Creating the '{}' table...", tableName);
        final String createTable = String.format("CREATE TABLE %s", tableName);
        try (Result result = txn.execute(createTable)) {
            log.info("{} table created successfully.", tableName);
            return toIonValues(result).size();
        }
    }

    /**
     * Convert the result set into a list of IonValues.
     */
    public static List<IonValue> toIonValues(Result result) {
        final List<IonValue> valueList = new ArrayList<>();
        result.iterator().forEachRemaining(valueList::add);
        return valueList;
    }

    public static String getDocumentId(TransactionExecutor txn, String tableName, String identifier, String value) {
        try {
            final List<IonValue> parameters = Collections.singletonList((new IonObjectMapper()).writeValueAsIonValue(value));
            final String query = String.format("SELECT metadata.id FROM _ql_committed_%s AS p WHERE p.data.%s = ?", tableName, identifier);
            try (Result result = txn.execute(query, parameters)) {
                if (result.isEmpty()) {
                    throw new RuntimeException("Unable to retrieve document ID using " + value);
                }
                final IonReader reader = IonReaderBuilder.standard().build(result.iterator().next());
                reader.next();
                reader.stepIn();
                reader.next();
                return reader.stringValue();
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
    public static void main(String... args) {
        try {
            create(LEDGER_NAME);
            waitForActive(LEDGER_NAME);

        } catch (Exception e) {
            log.error("Unable to create the ledger!", e);
        }
        
        try {
            session = createQldbSession();
        } catch (QldbClientException e) {
            log.error("Unable to create session.", e);
        }

        try {
            session.execute(txn -> {
                createTable(txn, "Person");
                createTable(txn, "Organization");
            }, r -> log.info("Tables created successfully!"));
        } catch (Exception e) {
            log.error("Errors creating tables.", e);
        }
    }

}
