package ch.admin.bag.covidcertificate.backend.verifier.ws.controller;

import ch.admin.bag.covidcertificate.backend.verifier.data.PushRegistrationDataService;
import ch.admin.bag.covidcertificate.backend.verifier.model.push_registration.PushRegistration;
import ch.ubique.openapi.docannotations.Documentation;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("trust/v1")
public class PushRegistrationController {

    private final PushRegistrationDataService pushRegistrationDataService;

    public PushRegistrationController(PushRegistrationDataService pushRegistrationDataService) {
        this.pushRegistrationDataService = pushRegistrationDataService;
    }

    @PostMapping(
            value = "/register",
            consumes = {"application/json"})
    @Documentation(
            description = "Push registration",
            responses = {"200 => success", "400 => Error"})
    public @ResponseBody ResponseEntity<Void> registerPush(
            @RequestBody final PushRegistration pushRegistration) {
        pushRegistrationDataService.upsertPushRegistration(pushRegistration);
        return ResponseEntity.ok().build();
    }
}
