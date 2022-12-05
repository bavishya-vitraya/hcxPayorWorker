package hcxPayor.hcxpayorworker.repository;


import hcxPayor.hcxpayorworker.model.PreAuthRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PreAuthRequestRepo extends MongoRepository<PreAuthRequest,String> {
    PreAuthRequest findPreAuthRequestById(String id);
}
