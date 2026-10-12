package org.snomed.cdsservice.model;

import org.hl7.fhir.r4.model.Coding;

import java.util.Collection;
import java.util.Set;

public class MedicationAllergyCDSTrigger extends CDSTrigger {

    public MedicationAllergyCDSTrigger(String medicationLabel, Collection<Coding> medicationCodings, String allergenLabel, Collection<Coding> allergenCodings, CDSCard card) {
        super(medicationLabel, medicationCodings, allergenLabel, allergenCodings, card);
    }

    @Override
    public CDSCard createRelevantCard(Set<Coding> allergyCodings, Set<Coding> draftMedicationOrderCodings) {
        Collection<Coding> allergenIntersection = getIntersection(allergyCodings, getConditionCodings());
        Collection<Coding> medicationIntersection = getIntersection(draftMedicationOrderCodings, getMedicationCodings());

        if (!allergenIntersection.isEmpty() && !medicationIntersection.isEmpty()) {
            CDSCard cardInstance = getCard().cloneCard();

            cardInstance.setSummary(processTextTemplate(cardInstance.getSummary(), allergenIntersection, medicationIntersection));
            cardInstance.setDetail(processTextTemplate(cardInstance.getDetail(), allergenIntersection, medicationIntersection));
            addReferenceMedicationToCDSCard(medicationIntersection, cardInstance);

            return cardInstance;
        } else {
            return null;
        }
    }

    @Override
    String processTextTemplate(String text, Collection<Coding> allergenIntersection, Collection<Coding> medicationIntersection) {
        if (text == null) {
            return null;
        }

        text = text.replace("{{RuleMedication}}", getMedicationLabel());
        text = text.replace("{{ActualMedication}}", toHumanReadable(medicationIntersection, getMedicationLabel()));
        text = text.replace("{{RuleAllergen}}", getConditionLabel());
        text = text.replace("{{ActualAllergen}}", toHumanReadable(allergenIntersection, getConditionLabel()));

        return text;
    }
}