import ca.uhn.fhir.context.FhirContext;
import java.util.List;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RequestGroup;
import org.hl7.fhir.r4.model.Task;

public class FhirStarter {
  public static void main(String[] args) {
    Patient patient = new Patient();
    patient.setId("patientId1");

    ////Question: The first one seems to give me the Basic resource, which is not what I want. Is the latter the preferred way?
    //.setId(IdType.newRandomUuid())
    //.addIdentifier(new Identifier().setValue(IdType.newRandomUuid().toString()))

    var module1 = new ActivityDefinition()
        .setKind(ActivityDefinition.ActivityDefinitionKind.TASK)
        .addIdentifier(new Identifier().setValue(IdType.newRandomUuid().toString()))
        .setUrl("url/to/module1");
    var task1 = new ActivityDefinition()
        .setKind(ActivityDefinition.ActivityDefinitionKind.TASK)
        .setUrl("url/to/task1");
    var blackBoxOfContent = new RelatedArtifact()
        .setUrl("url/to/blackboxofcontent")
        .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON);
    var subtask1_1 = new ActivityDefinition()
        .setKind(ActivityDefinition.ActivityDefinitionKind.TASK)
        .addRelatedArtifact(blackBoxOfContent)
        .setUrl("url/to/subtask1_1");
    var video_meeting1 =
        new ActivityDefinition()
            .setKind(ActivityDefinition.ActivityDefinitionKind.APPOINTMENT)
            .setUrl("url/to/meetingId");

    var planDefinition = new PlanDefinition();
    var module1action =
        new PlanDefinition.PlanDefinitionActionComponent()
            .setDescription("module1")
            .setDefinition(new CanonicalType(module1.getUrl()));
    var task1action =
        new PlanDefinition.PlanDefinitionActionComponent()
            .setDescription("task1")
            .setDefinition(new CanonicalType(task1.getUrl()));
    var subtask1_1action =
        new PlanDefinition.PlanDefinitionActionComponent()
            .setDescription("subtask 1.1")
            .setDefinition(new CanonicalType(subtask1_1.getUrl()));
    var video_meeting1action =
        new PlanDefinition.PlanDefinitionActionComponent()
            .setDescription("video meeting")
            .setDefinition(new CanonicalType(video_meeting1.getUrl()));
    task1action.addAction(subtask1_1action);
    module1action.addAction(task1action);
    module1action.addAction(video_meeting1action);
    planDefinition.addAction(module1action);
    planDefinition.setDescription("iCBT Anxiety");

    //Question 1: Why don't these show up when inspecting the json encoded PlanDefinition?
    planDefinition.setContained(List.of(module1, task1, subtask1_1, video_meeting1));

//    //Question 3, why does this not work?
//    PlanDefinition.PlanDefinitionGoalComponent goal =
//        new PlanDefinition.PlanDefinitionGoalComponent().setCategory(GoalCategory.BEHAVIORAL);
//    planDefinition.addGoal(goal);

    var carePlan = new CarePlan();
    carePlan.setStatus(CarePlan.CarePlanStatus.ACTIVE);
    carePlan.setSubject(new Reference(patient.getId()));
    carePlan.addInstantiatesCanonical(planDefinition.getUrl());

    var requestGroup = new RequestGroup();
    requestGroup.setId("idToRequestGroup");
    requestGroup.setSubject(new Reference(patient.getId()));
    requestGroup.addInstantiatesCanonical(planDefinition.getUrl());
    traversePlanDefinitionAndPopulateRequestGroupInSomeSmartWay(planDefinition, requestGroup);
    carePlan.addActivity(new CarePlan.CarePlanActivityComponent().setReference(new Reference(requestGroup.getId())));

    var bundle = new Bundle();
    bundle.addEntry(new Bundle.BundleEntryComponent().setResource(planDefinition));
    bundle.addEntry(new Bundle.BundleEntryComponent().setResource(requestGroup));
    bundle.addEntry(new Bundle.BundleEntryComponent().setResource(carePlan));

    var ctx = FhirContext.forR4();
    var parser = ctx.newJsonParser();
    parser.setPrettyPrint(true);
    String serialized = parser.encodeResourceToString(bundle);
    System.out.println(serialized);
  }

  private static void traversePlanDefinitionAndPopulateRequestGroupInSomeSmartWay(PlanDefinition planDefinition,
                                                                                  RequestGroup requestGroup) {
    var module1task = new Task()
        .addIdentifier(new Identifier().setValue("url/to/module1task"))
        .addNote(new Annotation().setText("this is a task"));
    requestGroup.addContained(module1task);
    //Question 2: Does the resoruce target not appear in the RequestGroup?
    requestGroup.addAction(new RequestGroup.RequestGroupActionComponent()
        .setResourceTarget(module1task)
        .setTitle("Task for module 1"));
  }
}
