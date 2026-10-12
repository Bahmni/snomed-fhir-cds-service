package org.snomed.cdsservice.service.medication;

import org.hl7.fhir.r4.model.Coding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.snomed.cdsservice.model.CDSTrigger;
import org.snomed.cdsservice.service.tsclient.FHIRTerminologyServerClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MedicationAllergyRuleLoaderServiceTest {

    private static final String NSAID_VALUE_SET = "http://snomed.info/sct?fhir_vs=isa/372665008";

    private final FHIRTerminologyServerClient tsClient = Mockito.mock(FHIRTerminologyServerClient.class);
    private final MedicationAllergyRuleLoaderService loader = new MedicationAllergyRuleLoaderService();

    private List<CDSTrigger> load(String tsvPath) throws Exception {
        when(tsClient.expandValueSet(anyString())).thenReturn(Set.of(new Coding("http://snomed.info/sct", "372665008", "NSAID")));
        ReflectionTestUtils.setField(loader, "tsClient", tsClient);
        ReflectionTestUtils.setField(loader, "tsvPath", tsvPath);
        return loader.loadTriggers();
    }

    @Test
    void shouldLoadNsaidRuleFromShippedTsv() throws Exception {
        List<CDSTrigger> triggers = load("CDS_Medication-Allergy_Cards.tsv");

        assertEquals(1, triggers.size());
        verify(tsClient, Mockito.times(2)).expandValueSet(NSAID_VALUE_SET);
    }

    @Test
    void shouldStripSnomedTermAfterPipe(@TempDir Path dir) throws Exception {
        String header = "UUID\tMedication\tMedication SNOMED Code\tAllergen\tAllergen SNOMED Code\tCard Indicator\tCard Summary\tCard Detail\tSource\tSource Link\n";
        String row = "id\tNSAID\t372665008 | NSAID\tNSAID\t372665008 | NSAID\twarning\tsummary\tdetail\tsource\tlink\n";
        Path tsv = Files.writeString(dir.resolve("rules.tsv"), header + row);

        assertEquals(1, load(tsv.toString()).size());
        verify(tsClient, Mockito.times(2)).expandValueSet(NSAID_VALUE_SET);
    }
}
