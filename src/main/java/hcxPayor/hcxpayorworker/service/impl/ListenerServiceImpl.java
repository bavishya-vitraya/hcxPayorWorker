package hcxPayor.hcxpayorworker.service.impl;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import hcxPayor.hcxpayorworker.dto.*;
import hcxPayor.hcxpayorworker.dto.Procedure;
import hcxPayor.hcxpayorworker.model.PreAuthRequest;
import hcxPayor.hcxpayorworker.model.PreAuthResponse;
import hcxPayor.hcxpayorworker.payorEnum.ClaimFlowType;
import hcxPayor.hcxpayorworker.payorEnum.PolicyType;
import hcxPayor.hcxpayorworker.payorEnum.VitrayaRoomCategory;
import hcxPayor.hcxpayorworker.repository.PreAuthRequestRepo;
import hcxPayor.hcxpayorworker.repository.PreAuthResponseRepo;
import hcxPayor.hcxpayorworker.service.ListenerService;
import hcxPayor.hcxpayorworker.utils.Constants;
import hcxPayor.hcxpayorworker.utils.DateUtils;
import io.hcxprotocol.impl.HCXIncomingRequest;
import io.hcxprotocol.impl.HCXOutgoingRequest;
import io.hcxprotocol.init.HCXIntegrator;
import io.hcxprotocol.utils.JSONUtils;
import io.hcxprotocol.utils.Operations;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.codesystems.Adjudication;
import org.hl7.fhir.r4.model.codesystems.ClaimType;
import org.hl7.fhir.r4.model.codesystems.ProcessPriority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
public class ListenerServiceImpl implements ListenerService {

    @Autowired
    private PreAuthRequestRepo preAuthRequestRepo;
    @Autowired
    private PreAuthResponseRepo preAuthResponseRepo;
    @Override
    public boolean generateVhiRequest(Message msg) {
        PreAuthRequest preAuthRequest;
        VhiPreAuthRequest vhiPreAuthRequest;
        String reqType = msg.getMessageType();
        if (reqType.equalsIgnoreCase(Constants.PRE_AUTH)) {

            try {
                preAuthRequest = preAuthRequestRepo.findPreAuthRequestById(msg.getReferenceId());
                vhiPreAuthRequest=buildVhiPreAuthRequest(preAuthRequest.getPreAuthRequest());
                log.info("PreAuthReq:{}", preAuthRequest);
                log.info("vhiPreAuthRequest{}",new Gson().toJson(vhiPreAuthRequest));
            }
            catch(Exception e){
                log.error("error in fetching preAuth request", e);
            }

        }
    return true;
    }

    @Override
    public void generateHcxResponse(Message msg) throws Exception {
        String responseType = msg.getMessageType();
        try {
            if (responseType.equalsIgnoreCase(Constants.PRE_AUTH_RESPONSE)) {
                PreAuthResponse preAuthResponse = preAuthResponseRepo.findPreAuthResponseById(msg.getReferenceId());
                String responseFhirpayload = buildFhirClaimResponse(preAuthResponse.getPreAuthVhiResponse());
                HCXIntegrator.init(setPayorConfig());
                HCXOutgoingRequest hcxOutgoingRequest = new HCXOutgoingRequest();
                Map<String,Object> output = new HashMap<>();
                Operations operation = Operations.PRE_AUTH_ON_SUBMIT;
                String status = "response.partial";
                String actionJwe = preAuthResponse.getInputFhirRequest();
                Boolean res = hcxOutgoingRequest.generate(responseFhirpayload,operation,actionJwe,status,output);
                System.out.println("{}"+res+output);
                if (res) {
                    preAuthResponse.setOutputFhirResponse((String) output.get("payload"));
                    preAuthResponseRepo.save(preAuthResponse);
                }
            }
        }
        catch(Exception e){
            log.error("Error in generating fhir pre Auth response", e);
        }
    }
    public String buildFhirClaimResponse(PreAuthVhiResponse vhiResponse) throws Exception {
        log.info("vhiResponse:{}",vhiResponse);
        AttachmentResDTO attachmentResDTO = new AttachmentResDTO();
        attachmentResDTO.setQuery(vhiResponse.getQuery());
        attachmentResDTO.setFiles(vhiResponse.getFiles());

        String attachmentString = new Gson().toJson(attachmentResDTO);
        String encodedAttachement = Base64.getUrlEncoder().encodeToString(attachmentString.getBytes());

        Patient patient = new Patient();// should fetch from claim request
        patient.setId("Patient/1");

        Organization organization = new Organization(); // should fetch from claim request
        organization.setId("organization/1");
        organization.setName("Test-HOS01");

        Claim claimRequest = new Claim(); // should fetch from claim request
        claimRequest.setId("Claim/1");
        claimRequest.setUse(Claim.Use.PREAUTHORIZATION);
        claimRequest.setId("Claim/1");
        claimRequest.setCreated(new Date());
        claimRequest.setStatus(Claim.ClaimStatus.ACTIVE);
        claimRequest.setType(new CodeableConcept(new Coding().setCode(ClaimType.INSTITUTIONAL.toCode()).setSystem("http://terminology.hl7.org/CodeSystem/claim-type")));
        claimRequest.setPriority(new CodeableConcept(new Coding().setSystem("http://terminology.hl7.org/CodeSystem/processpriority").setCode(ProcessPriority.NORMAL.toCode())));
        claimRequest.setPatient(new Reference(patient.getId()));
        claimRequest.setProvider(new Reference(organization.getId()));
        claimRequest.addInsurance().setSequence(1).setFocal(true).setCoverage(new Reference("Coverage/1"));


        ClaimResponse claimResponse = new ClaimResponse();
        claimResponse.setId("ClaimResponse/1");
        claimResponse.addIdentifier().setValue(vhiResponse.getClaimNumber());
        claimResponse.setPreAuthRef(vhiResponse.getClaimNumber());
        claimResponse.setOutcome(ClaimResponse.RemittanceOutcome.COMPLETE); // no approved enum provided
        claimResponse.setDisposition(vhiResponse.getClaimStatusInString());
        claimResponse.addProcessNote().setType(Enumerations.NoteType.DISPLAY).setText(encodedAttachement);
        claimResponse.addTotal().setAmount(new Money().setCurrency("INR").setValue(vhiResponse.getApprovedAmount())).setCategory(new CodeableConcept(new Coding().setCode(Adjudication.ELIGIBLE.toCode()).setSystem("http://terminology.hl7.org/CodeSystem/adjudication")));
        claimResponse.setStatus(ClaimResponse.ClaimResponseStatus.ACTIVE);
        claimResponse.setType(new CodeableConcept(new Coding().setCode(ClaimType.INSTITUTIONAL.toCode()).setSystem("http://terminology.hl7.org/CodeSystem/claim-type")));
        claimResponse.setUse(ClaimResponse.Use.PREAUTHORIZATION);
        claimResponse.setCreated(new Date());
        claimResponse.setPatient(new Reference(patient.getId()));
        claimResponse.setRequestor(new Reference(organization.getId()));
        claimResponse.setInsurer(new Reference(organization.getId()));
        claimResponse.setRequest(new Reference(claimRequest.getId()));


        Composition composition= new Composition();
        composition.setId("composition/" + UUID.randomUUID().toString());
        composition.setStatus(Composition.CompositionStatus.FINAL);
        composition.getType().addCoding().setCode("HCXClaimResponse").setSystem("https://hcx.org/document-types").setDisplay("Claim Response");
        composition.setDate(new Date());
        composition.addAuthor().setReference("Organization/1");
        composition.setTitle("Claim Response");
        composition.addSection().addEntry().setReference("ClaimResponse/1");

        FhirContext fhirctx = FhirContext.forR4();
        Bundle bundle = new Bundle();
        bundle.setId(UUID.randomUUID().toString());
        bundle.setType(Bundle.BundleType.DOCUMENT);
        bundle.getIdentifier().setSystem("https://www.tmh.in/bundle").setValue(bundle.getId());
        bundle.setTimestamp(new Date());
        bundle.addEntry().setFullUrl(composition.getId()).setResource(composition);
        bundle.addEntry().setFullUrl(claimRequest.getId()).setResource(claimRequest);
        bundle.addEntry().setFullUrl(patient.getId()).setResource(patient);
        bundle.addEntry().setFullUrl(organization.getId()).setResource(organization);
        bundle.addEntry().setFullUrl(claimResponse.getId()).setResource(claimResponse);

        IParser p = fhirctx.newJsonParser().setPrettyPrint(true);
        String messageString = p.encodeResourceToString(bundle);
        System.out.println("Generated Fhir PreAuth Response" + messageString);
        return messageString;
    }

    public  Map<String, Object> setPayorConfig() throws IOException {
        Map<String, Object> config = new HashMap<>();
        File file = new ClassPathResource("keys/vitraya-mock-payor-private-key.pem").getFile();
        String privateKey= FileUtils.readFileToString(file);
        config.put("protocolBasePath", "http://staging-hcx.swasth.app/api/v0.7");
        config.put("authBasePath","http://a9dd63de91ee94d59847a1225da8b111-273954130.ap-south-1.elb.amazonaws.com:8080/auth/realms/swasth-health-claim-exchange/protocol/openid-connect/token");
        config.put("participantCode","1-434d79f6-aad8-48bc-b408-980a4dbd90e2");
        config.put("username", "vitrayahcxpayor1@vitrayatech.com");
        config.put("password","BkYJHwm64EEn8B8");
        config.put("encryptionPrivateKey", privateKey);
        config.put("igUrl", "https://ig.hcxprotocol.io/v0.7");
        return config;
    }

    @Override
    public  VhiPreAuthRequest buildVhiPreAuthRequest(String request) throws Exception {
        VhiPreAuthRequest vhiPreAuthRequest=new VhiPreAuthRequest();
        FhirContext fhirctx = FhirContext.forR4();
        IParser parser = fhirctx.newJsonParser().setPrettyPrint(true);
        Bundle bundle = parser.parseResource(Bundle.class, request);
        org.hl7.fhir.r4.model.Claim claim;
        hcxPayor.hcxpayorworker.dto.Claim vhiClaim= new hcxPayor.hcxpayorworker.dto.Claim();
        ClaimIllnessTreatmentDetails claimIllnessTreatmentDetails= new ClaimIllnessTreatmentDetails();
        ClaimAdmissionDetails claimAdmissionDetails= new 	ClaimAdmissionDetails();
        HospitalServiceType hospitalServiceType= new  HospitalServiceType();
        ProcedureMethod procedureMethod= new ProcedureMethod();
        Procedure vhiProcedure= new Procedure();
        Illness illness= new Illness();
        for(Bundle.BundleEntryComponent entryComponent: bundle.getEntry()) {
            String resourceType = entryComponent.getResource().getResourceType().toString();
            if (resourceType.equalsIgnoreCase("Claim")) {
                claim= (org.hl7.fhir.r4.model.Claim) entryComponent.getResource();
                vhiPreAuthRequest.setClaimFlowType(ClaimFlowType.PRE_AUTH);
                vhiClaim.setCreatedDate(DateUtils.formatDate(claim.getCreated()));
                List<org.hl7.fhir.r4.model.Claim.ItemComponent> itemList = claim.getItem();
                for(org.hl7.fhir.r4.model.Claim.ItemComponent item:itemList){
                    List<Coding> productCodingList =item.getProductOrService().getCoding();
                    for(Coding coding: productCodingList ){
                        if(coding.getDisplay().equalsIgnoreCase("Expense")){
                            hospitalServiceType.setRoomTariffPerDay(item.getUnitPrice().getValue());
                        }
                    }

                }

                List<org.hl7.fhir.r4.model.Claim.SupportingInformationComponent> supportingInfoList=new ArrayList<>();
                supportingInfoList=claim.getSupportingInfo();
                for(org.hl7.fhir.r4.model.Claim.SupportingInformationComponent supportingInfo:supportingInfoList){
                    List<Coding> codingList= supportingInfo.getCode().getCoding();
                    for(Coding coding:codingList){
                        if(coding.getCode().equalsIgnoreCase("ONS-6")){
                            claimAdmissionDetails.setIcuStay(supportingInfo.getValueBooleanType().booleanValue());
                        }
                        else if(coding.getCode().equalsIgnoreCase("ONS-1")){
                            claimAdmissionDetails.setAdmissionDate(DateUtils.formatDate(supportingInfo.getTimingDateType().getValue()));
                        }
                        else if(coding.getCode().equalsIgnoreCase("ONS-2")){
                            claimAdmissionDetails.setDischargeDate(DateUtils.formatDate(supportingInfo.getTimingDateType().getValue()));
                        }
                        else if(coding.getCode().equalsIgnoreCase("INF-1")){
                            String encodedString= supportingInfo.getValueStringType().toString();
                            byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
                            String attachment = new String(decodedBytes);
                            AttachmentDTO attachmentDTO =  new Gson().fromJson(attachment, new TypeToken<AttachmentDTO>() {
                            }.getType());
                            vhiClaim.setId(attachmentDTO.getParentTableId());
                            vhiClaim.setDeleted(attachmentDTO.isDeleted());
                            vhiClaim.setUpdatedDate(DateUtils.formatDate(attachmentDTO.getUpdatedDate()));
                            vhiClaim.setState(attachmentDTO.getState());
                            vhiClaim.setStatus(attachmentDTO.getStatus());
                            vhiClaim.setAge(attachmentDTO.getAge());
                            vhiClaim.setProductCode(attachmentDTO.getProductCode());
                            vhiClaim.setMedicalEventId(attachmentDTO.getMedicalEventId());
                            vhiClaim.setPolicyInceptionDate(attachmentDTO.getPolicyInceptionDate());

                            claimIllnessTreatmentDetails.setClaimId(attachmentDTO.getParentTableId());
                            claimIllnessTreatmentDetails.setChronicIllnessDetails(attachmentDTO.getChronicIllnessDetailsJSON().getChronicIllnessList().toString());
                            claimIllnessTreatmentDetails.setProcedureCorporateMappingId(attachmentDTO.getProcedureCorporateMappingId());
                            claimIllnessTreatmentDetails.setProcedureId(attachmentDTO.getProcedureId());
                            claimIllnessTreatmentDetails.setLeftImplant(attachmentDTO.getLeftImplant());
                            claimIllnessTreatmentDetails.setRightImplant(attachmentDTO.getRightImplant());
                            if(attachmentDTO.getChronicIllnessDetailsJSON().getChronicIllnessList()!=null) {
                                claimIllnessTreatmentDetails.setChronicIllnessDetailsJSON(attachmentDTO.getChronicIllnessDetailsJSON());
                            }

                            claimAdmissionDetails.setClaimId(attachmentDTO.getParentTableId());
                            claimAdmissionDetails.setHospitalServiceTypeId(attachmentDTO.getHospitalServiceTypeId());
                            claimAdmissionDetails.setStayDuration(attachmentDTO.getStayDuration());
                            claimAdmissionDetails.setPackageAmount(attachmentDTO.getPackageAmount());
                            claimAdmissionDetails.setCostEstimation(attachmentDTO.getCostEstimation());
                            claimAdmissionDetails.setIcuStayDuration(attachmentDTO.getIcuStayDuration());
                            claimAdmissionDetails.setIcuServiceTypeId(attachmentDTO.getIcuServiceTypeId());
                            claimAdmissionDetails.setRoomType(attachmentDTO.getRoomType());

                            hospitalServiceType.setRoomType(attachmentDTO.getRoomType());
                            hospitalServiceType.setVitrayaRoomCategory(VitrayaRoomCategory.valueOf(attachmentDTO.getVitrayaRoomCategory()));
                            hospitalServiceType.setInsurerRoomType(attachmentDTO.getInsurerRoomType());
                            hospitalServiceType.setSinglePrivateAC(attachmentDTO.isSinglePrivateAC());
                            hospitalServiceType.setServiceType(attachmentDTO.getServiceType());

                            illness.setIllnessCategoryId(attachmentDTO.getIllnessCategoryId());
                            illness.setIllnessName(attachmentDTO.getIllnessName());
                            illness.setDefaultICDCode(attachmentDTO.getDefaultICDCode());

                            procedureMethod.setProcedureCode(attachmentDTO.getProcedureCode());

                            vhiPreAuthRequest.setServiceTypeId(attachmentDTO.getServiceTypeId());
                            vhiPreAuthRequest.setDocumentMasterList(attachmentDTO.getDocumentMasterList());
                        }
                        else if(coding.getCode().equalsIgnoreCase("TRD-3")){
                            claimIllnessTreatmentDetails.setLineOfTreatmentDetails(supportingInfo.getValueStringType().getValue());
                        }
                    }


                }

            }
            else if(resourceType.equalsIgnoreCase("patient")){
                Patient patient= (Patient) entryComponent.getResource();
                vhiClaim.setHospitalPatientId(patient.getIdentifier().get(0).getValue());
                vhiClaim.setDob(DateUtils.formatDate(patient.getBirthDate()));
                vhiClaim.setGender(patient.getGenderElement().getValueAsString());
                vhiClaim.setPatientName(patient.getName().get(0).getNameAsSingleString());
                vhiClaim.setPolicyHolderName(patient.getName().get(0).getNameAsSingleString());
                List<ContactPoint> telecomList=patient.getTelecom();
                for(ContactPoint contactPoint:telecomList){
                    if(contactPoint.getSystem()== ContactPoint.ContactPointSystem.PHONE){
                        vhiClaim.setPatient_mobile_no(contactPoint.getValue());
                    }
                    if(contactPoint.getSystem()== ContactPoint.ContactPointSystem.EMAIL){
                        vhiClaim.setPatient_email_id(contactPoint.getValue());
                    }

                }
                vhiClaim.setAttendent_mobile_no(patient.getContact().get(0).getTelecom().get(0).getValue());

            }
            else if(resourceType.equalsIgnoreCase("procedure")){
               org.hl7.fhir.r4.model.Procedure procedure= (org.hl7.fhir.r4.model.Procedure) entryComponent.getResource();
                vhiProcedure.setDescription(procedure.getNote().get(0).getText());
                vhiProcedure.setName(procedure.getCode().getText());
                procedureMethod.setProcedureMethodName(procedure.getCode().getText());

            }
            else if(resourceType.equalsIgnoreCase("organization")){
                Organization organization= new Organization();
                if(entryComponent.getFullUrl().contains("InsurerOrganization")){
                    organization= (Organization) entryComponent.getResource();
                    vhiClaim.setInsuranceAgencyId(Integer.valueOf(organization.getIdentifier().get(0).getValue()));
                }
                else if(entryComponent.getFullUrl().contains("ProviderOrganization/1")){
                    organization= (Organization) entryComponent.getResource();
                    List<Identifier> identifierList= organization.getIdentifier();
                    for(Identifier identifier:identifierList){
                        List<Coding> codingList=identifier.getType().getCoding();
                        for(Coding code:codingList){
                            if(code.getCode().equalsIgnoreCase("PRN")){
                                vhiClaim.setHospitalId(Integer.valueOf(identifier.getValue()));
                            }
                        }
                    }
                    vhiClaim.setCityName(organization.getContact().get(0).getPurpose().getText());
                }

            }
            else if(resourceType.equalsIgnoreCase("Practitioner")){
                Practitioner practitioner= (Practitioner) entryComponent.getResource();
                List<HumanName> nameList=practitioner.getName();
                List<Practitioner.PractitionerQualificationComponent> qualificationList= practitioner.getQualification();
                DoctorDetailsDto doctor= new DoctorDetailsDto();
                for(HumanName name:nameList){
                    doctor.setDoctorName(name.getGivenAsSingleString());
                }
                for(Practitioner.PractitionerQualificationComponent qualification:qualificationList){
                    doctor.setQualification(qualification.getCode().getText());
                }
                claimIllnessTreatmentDetails.setDoctorsDetails(new Gson().toJson(doctor));

                List<Identifier> identifierList= practitioner.getIdentifier();
                for(Identifier identifier:identifierList){
                    List<Coding> codingList=identifier.getType().getCoding();
                    for(Coding code:codingList){
                        if(code.getCode().equalsIgnoreCase("PLAC")){
                            vhiClaim.setCreatorId(Long.valueOf(identifier.getValue()));
                        }
                    }
                }


            }
            else if(resourceType.equalsIgnoreCase("Condition")){
                Condition condition= (Condition) entryComponent.getResource();
                claimIllnessTreatmentDetails.setDateOfDiagnosis(DateUtils.formatDate(condition.getRecordedDate()));
            }
            else if(resourceType.equalsIgnoreCase("Coverage")){
                Coverage coverage= (Coverage) entryComponent.getResource();
                vhiClaim.setMedicalCardId(coverage.getSubscriberId());
                vhiClaim.setPolicyNumber(coverage.getIdentifier().get(0).getValue());
                vhiClaim.setPolicyType(PolicyType.valueOf(coverage.getType().getText()));
                vhiClaim.setPolicyEndDate(DateUtils.formatDate(coverage.getPeriod().getEnd()));
                vhiClaim.setPolicyName(coverage.getClass_().get(0).getValue());
                vhiClaim.setPolicyStartDate(DateUtils.formatDate(coverage.getPeriod().getStart()));
            }

        }
        vhiPreAuthRequest.setClaim(vhiClaim);
        vhiPreAuthRequest.setClaimIllnessTreatmentDetails(claimIllnessTreatmentDetails);
        vhiPreAuthRequest.setClaimAdmissionDetails(claimAdmissionDetails);
        vhiPreAuthRequest.setHospitalServiceType(hospitalServiceType);
        vhiPreAuthRequest.setProcedure(vhiProcedure);
        vhiPreAuthRequest.setProcedureMethod(procedureMethod);
        vhiPreAuthRequest.setIllness(illness);

        return  vhiPreAuthRequest;
    }


}
