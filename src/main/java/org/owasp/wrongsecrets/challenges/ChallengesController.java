package org.owasp.wrongsecrets.challenges;

import org.owasp.wrongsecrets.ScoreCard;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class ChallengesController {

    private final ScoreCard scoreCard;
    private final List<ChallengeUI> challenges;

    public ChallengesController(ScoreCard scoreCard, List<ChallengeUI> challenges) {
        this.scoreCard = scoreCard;
        this.challenges = challenges;
    }

    @GetMapping
    public String explanation(@PathVariable Integer id) {
        return challenges.get(id - 1).getExplanation();
    }

    @GetMapping("/spoil-{id}")
    public String spoiler(Model model, @PathVariable Integer id) {
        var challenge = challenges.get(id - 1).getChallenge();
        model.addAttribute("solution", challenge.spoiler());
        return "spoil";
    }

    @GetMapping("/challenge/{id}")
    public String challenge(Model model, @PathVariable Integer id) {
        var challenge = challenges.get(id - 1);

        model.addAttribute("challengeForm", new ChallengeForm(""));
        model.addAttribute("challenge", challenge);

        model.addAttribute("answerCorrect", null);
        model.addAttribute("answerIncorrect", null);
        model.addAttribute("solution", null);

        includeScoringStatus(model, challenge.getChallenge());
        addWarning(challenge.getChallenge(), model);

        return "challenge";
    }

    @PostMapping(value = "/challenge/{id}", params = "action=reset")
    public String reset(@ModelAttribute ChallengeForm challengeForm, @PathVariable Integer id, Model model) {
        var challenge = challenges.get(id - 1);
        scoreCard.reset(challenge.getChallenge());

        model.addAttribute("challenge", challenge);
        includeScoringStatus(model, challenge.getChallenge());

        return "challenge";
    }


    @PostMapping(value = "/challenge/{id}", params = "action=submit")
    public String postController(@ModelAttribute ChallengeForm challengeForm, Model model, @PathVariable Integer id) {
        var challenge = challenges.get(id - 1);

        if (challenge.getChallenge().solved(challengeForm.solution())) {
            model.addAttribute("answerCorrect", "Your answer is correct!");
        } else {
            model.addAttribute("answerIncorrect", "Your answer is incorrect, try harder ;-)");
        }

        model.addAttribute("challenge", challenge);
        includeScoringStatus(model, challenge.getChallenge());
        return "challenge";
    }

    private void includeScoringStatus(Model model, Challenge challenge) {
        model.addAttribute("totalPoints", scoreCard.getTotalReceivedPoints());
        model.addAttribute("progress", "" + scoreCard.getProgress());

        if (scoreCard.getChallengeCompleted(challenge)) {
            model.addAttribute("challengeCompletedAlready", "This exercise is already completed");
        }
    }

    private void addWarning(Challenge challenge, Model model) {
        if (!challenge.environmentSupported())
            model.addAttribute("runtimeWarning", switch (challenge.getEnvironment()) {
                case DOCKER -> "We are running outside of a docker container. Please run this in a container as explained in the README.md.";
                case K8S -> "We are running outside of a K8s cluster. Please run this in the K8s cluster as explained in the README.md.";
                case K8S_VAULT -> "We are running outside of a K8s cluster with Vault. Please run this in the K8s cluster as explained in the README.md.";
                case CLOUD -> "We are running outside of a properly configured AWS or GCP environment. Please run this in an AWS or GCP environment as explained in the README.md.";
                case AWS -> "We are running outside of a properly configured AWS environment. Please run this in an AWS environment as explained in the README.md. GCP is not done yet";
            });
    }
}
