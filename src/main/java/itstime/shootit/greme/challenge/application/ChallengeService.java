package itstime.shootit.greme.challenge.application;

import itstime.shootit.greme.challenge.domain.Challenge;
import itstime.shootit.greme.challenge.dto.GetChallengeList;
import itstime.shootit.greme.challenge.infrastructure.ChallengePostRepository;
import itstime.shootit.greme.challenge.infrastructure.ChallengeUserRepository;
import itstime.shootit.greme.challenge.infrastructure.ChallengeRepository;
import itstime.shootit.greme.challenge.domain.ChallengeUser;
import itstime.shootit.greme.challenge.dto.GetChallengeSummaryRes;
import itstime.shootit.greme.challenge.dto.ChallengeTitle;
import itstime.shootit.greme.challenge.exception.FailAddChallengeException;
import itstime.shootit.greme.post.dto.ChallengeDTO;
import itstime.shootit.greme.post.dto.GetPostRes;
import itstime.shootit.greme.post.infrastructure.PostRepository;
import itstime.shootit.greme.user.domain.User;
import itstime.shootit.greme.user.dto.GetProfileRes;
import itstime.shootit.greme.user.exception.NotExistUserException;
import itstime.shootit.greme.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
//@Lazy
@Service
public class ChallengeService {

    private final ChallengeUserRepository challengeUserRepository;
    private final ChallengeRepository challengeRepository;
    private final ChallengePostRepository challengePostRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    @Transactional(readOnly = true)
    public List<GetChallengeSummaryRes> challenge(Long userId){
        if (!userRepository.existsById(userId)) {
            throw new NotExistUserException();
        }

        return challengeUserRepository.mfindChallenge(userId);
    }

    @Transactional(readOnly = true)
    public List<GetChallengeSummaryRes> joinChallenge(Long userId){
        if(!userRepository.existsById(userId)) {
            throw new NotExistUserException();
        }

        return challengeUserRepository.mfindJoinChallenge(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void addChallenge(String email, Long challengeId){
        User user = userRepository.findByEmail(email)
                .orElseThrow(NotExistUserException::new);

        if (challengeUserRepository.findByChallengeAndUser(user.getId(), challengeId) != null){  // 이미 챌린지 등록되어 있으면
            throw new FailAddChallengeException();
        }
        challengeUserRepository.save(ChallengeUser.builder()
                .challenge(challengeRepository.findById(challengeId).get())
                .user(user)
                .build());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteChallenge(String email, Long challengeId){
        User user = userRepository.findByEmail(email)
                .orElseThrow(NotExistUserException::new);
        ChallengeUser challengeUserEntity = challengeUserRepository.findByChallengeAndUser(challengeId, user.getId());

        challengeUserRepository.delete(challengeUserEntity);
    }

    @Transactional(readOnly = true)
    public List<ChallengeTitle> findJoinChallengeTitle(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(NotExistUserException::new);

        return challengeUserRepository.findJoinChallengeTitle(user.getId());
    }

    @Transactional(readOnly = true)
    public GetProfileRes showUserProfile(String email, Long user_id){
        User user = userRepository.findByEmail(email)
                .orElseThrow(NotExistUserException::new);

        List<GetChallengeSummaryRes> getChallengeSummaryRes = challengeUserRepository.findRecentJoinChallenge(user_id); // 이번달에 참여한 챌린지 가져오기
        List<GetPostRes> postRes = postRepository.findRecentPostByUserEmail(user_id);  // 가장 최근에 작성된 post 6개

        return GetProfileRes.builder()
                .profileImg(user.getProfileImg())
                .username(user.getUsername())
                .challengeSummary(getChallengeSummaryRes)
                .postRes(postRes).build();

    }

    @Transactional(readOnly = true)
    public ChallengeDTO showChallengeList(String email, Long challenge_id){
        User user = userRepository.findByEmail(email)
                .orElseThrow(NotExistUserException::new);

        boolean status = challengeUserRepository.existsByChallengeIdAndUserId(challenge_id, user.getId());  // 해당 챌린지에 참여하는지
//        System.out.println(status);
        List<GetChallengeList> challengeList = challengePostRepository.findAllImageByChallengeId(challenge_id);  // 챌린지 참여 목록
        System.out.println("참여목록 찾음");

        GetChallengeSummaryRes challengeSummary = challengeRepository.findById(challenge_id, GetChallengeSummaryRes.class); // 해당 챌린지 정보
        System.out.println("정보 찾음");


        return ChallengeDTO.builder()
                .status(status)
                .getChallengeLists(challengeList)
                .summaryRes(challengeSummary).build();

    }
}
