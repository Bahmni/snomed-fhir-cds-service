package org.snomed.cdsservice.service.medication;

import com.google.common.base.Strings;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.utilities.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.cdsservice.model.CDSCard;
import org.snomed.cdsservice.model.CDSIndicator;
import org.snomed.cdsservice.model.CDSSource;
import org.snomed.cdsservice.model.CDSTrigger;
import org.snomed.cdsservice.model.MedicationAllergyCDSTrigger;
import org.snomed.cdsservice.service.ServiceException;
import org.snomed.cdsservice.service.tsclient.FHIRTerminologyServerClient;
import org.snomed.cdsservice.util.SnomedValueSetUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class MedicationAllergyRuleLoaderService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String CONTRAINDICATION_ALERT_TYPE = "Contraindication";

    @Value("${rules.medication-allergy.tsv}")
    private String tsvPath;

    @Autowired
    private FHIRTerminologyServerClient tsClient;

    public List<CDSTrigger> loadTriggers() throws ServiceException {
        List<CDSTrigger> triggers = new ArrayList<>();
        try (FileInputStream file = new FileInputStream(tsvPath)) {
            CSVReader csvReader = new CSVReader(file);
            char TAB_DELIMITER = '\t';
            csvReader.setDelimiter(TAB_DELIMITER);
            String[] expectedHeadings = new String[]{
                    "UUID",
                    "Medication",
                    "Medication SNOMED Code",
                    "Allergen",
                    "Allergen SNOMED Code",
                    "Card Indicator",
                    "Card Summary",
                    "Card Detail",
                    "Source",
                    "Source Link"
            };

            csvReader.readHeaders();
            int rowNumber = 1;
            while (csvReader.line()) {
                String uuid = csvReader.cell(expectedHeadings[0]);
                String medicationLabel = csvReader.cell(expectedHeadings[1]);
                String medicationSnomedCode = csvReader.cell(expectedHeadings[2]);
                String allergenLabel = csvReader.cell(expectedHeadings[3]);
                String allergenSnomedCode = csvReader.cell(expectedHeadings[4]);
                String cardIndicator = csvReader.cell(expectedHeadings[5]);
                String cardSummary = csvReader.cell(expectedHeadings[6]);
                String cardDetail = csvReader.cell(expectedHeadings[7]);
                String source = csvReader.cell(expectedHeadings[8]);
                String sourceLink = csvReader.cell(expectedHeadings[9]);

                if (Strings.isNullOrEmpty(allergenSnomedCode) || Strings.isNullOrEmpty(medicationSnomedCode) || Strings.isNullOrEmpty(source)) {
                    logger.info("Ignoring row {}, allergenSnomedCode {} medicationSnomedCode {} source {} ", rowNumber, allergenSnomedCode, medicationSnomedCode, source);
                    continue;
                }

                if (allergenSnomedCode.contains("|") && !allergenSnomedCode.startsWith("ECL=")) {
                    allergenSnomedCode = allergenSnomedCode.substring(allergenSnomedCode.indexOf("|")).trim();
                }

                if (medicationSnomedCode.contains("|") && !medicationSnomedCode.startsWith("ECL=")) {
                    medicationSnomedCode = medicationSnomedCode.substring(medicationSnomedCode.indexOf("|")).trim();
                }

                CDSCard cdsCard = new CDSCard(uuid, cardSummary, cardDetail, CDSIndicator.valueOf(cardIndicator), new CDSSource(source, sourceLink), null, null, CONTRAINDICATION_ALERT_TYPE);
                Collection<Coding> allergenCodings = tsClient.expandValueSet(SnomedValueSetUtil.getSNOMEDValueSetURI(allergenSnomedCode));
                Collection<Coding> medicationCodings = tsClient.expandValueSet(SnomedValueSetUtil.getSNOMEDValueSetURI(medicationSnomedCode));
                logger.info("Created allergy trigger {} / {}", allergenLabel, medicationLabel);
                triggers.add(new MedicationAllergyCDSTrigger(medicationLabel, medicationCodings, allergenLabel, allergenCodings, cdsCard));

                rowNumber++;
            }
        } catch (FileNotFoundException e) {
            logger.warn("CDS medication-allergy rules file {} not found, skipping medication allergy rules.", tsvPath);
        } catch (Exception e) {
            throw new ServiceException("Failed to read CDS medication allergy rules from tab separated file", e);
        }
        return triggers;
    }
}