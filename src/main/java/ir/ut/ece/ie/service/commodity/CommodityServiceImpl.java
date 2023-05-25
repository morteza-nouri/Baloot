package ir.ut.ece.ie.service.commodity;

import ir.ut.ece.ie.api.dto.CommodityDTO;
import ir.ut.ece.ie.api.mapper.CommodityMapper;
import ir.ut.ece.ie.domain.commodity.Commodity;
import ir.ut.ece.ie.domain.commodity.Score;
import ir.ut.ece.ie.exception.OnlineShopException;
import ir.ut.ece.ie.repository.commodity.CommodityRepository;
import ir.ut.ece.ie.repository.commodity.ScoreRepository;
import ir.ut.ece.ie.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommodityServiceImpl implements CommodityService {
    private final CommodityRepository commodityRepository;
    private final ScoreRepository scoreRepository;
    private final UserRepository userRepository;
    private final CommodityMapper mapper;

    @Override
    public Commodity addCommodity(CommodityDTO commodityDTO) {
        Commodity commodity = mapper.toModel(commodityDTO);
        try {
            commodity = commodityRepository.save(commodity);
        } catch (Exception e) {
            throw new OnlineShopException(e.getMessage());
        }
        return commodity;
    }

    @Override
    public Optional<Commodity> getCommodityById(Long id) {
        return commodityRepository.findById(id);
    }

    @Override
    public List<Commodity> getCommodities() {
        return commodityRepository.findAll();
    }

    @Override
    public List<Commodity> getCommoditiesByProviderId(Integer id) {
        return (List<Commodity>) commodityRepository.findAllByProviderId(id);
    }

    @Override
    public List<Commodity> getCommoditiesByCategory(String category) {
        return (List<Commodity>) commodityRepository.findAllByCategoriesContainsIgnoreCase(category);
    }

    @Override
    public List<Commodity> getCommoditiesByNameContains(String searchStr) {
        return (List<Commodity>) commodityRepository.findAllByNameIsContainingIgnoreCase(searchStr);
    }

    @Override
    public List<Commodity> getCommoditiesInPriceRange(Long from, Long to) {
        if (from > to) throw new OnlineShopException("Invalid price range. From must be less than to");
        if (from < 0) throw new OnlineShopException("Invalid price range. From and to must be positive");
        return (List<Commodity>) commodityRepository.findAllByPriceIsBetween(from, to);
    }

    @Override
    public Commodity rateCommodity(Score score) {
        if (score.getScore() < 1 || score.getScore() > 10)
            throw new OnlineShopException("Invalid score.It must be between 1 and 10");
        Commodity commodity = getCommodityById(score.getCommodityId())
                .orElseThrow(() -> new OnlineShopException("Commodity not found"));
        userRepository.findById(score.getUsername()).orElseThrow(() -> new OnlineShopException("User not found"));
        scoreRepository.save(score);
        List<Score> scoreList = (List<Score>) scoreRepository.findAllByCommodityId(score.getCommodityId());
        int numOfRatings = scoreList.size();
        double sumOfScores = scoreList.stream().mapToInt(Score::getScore).sum();
        commodity.setRating(sumOfScores / numOfRatings);
        commodity.setRateCount(numOfRatings);
        commodityRepository.save(commodity);
        return commodity;
    }

    @Override
    public List<Commodity> getSuggestedCommodities(Long id) {
        //todo: can we write query directly to database?
        Commodity baseCommodity = getCommodityById(id).orElseThrow(() -> new OnlineShopException("Commodity not found"));
        List<Commodity> commodityList = new ArrayList<>(getCommodities());
        commodityList.sort((c1, c2) -> compareScore(c1, c2, baseCommodity));
        commodityList = commodityList.stream().filter(commodity -> !commodity.getId().equals(id)).limit(5).toList();
        return commodityList;
    }

    private double calculateScore(Commodity commodity, Commodity base) {
        double score = commodity.getRating();
        boolean haveIntersect = false;
        for (String c : commodity.getCategories()) {
            if (base.getCategories().contains(c)) {
                haveIntersect = true;
                break;
            }
        }
        score += haveIntersect ? 11 : 0;
        return score;
    }

    private int compareScore(Commodity c1, Commodity c2, Commodity base) {
        Double s1 = calculateScore(c1, base);
        Double s2 = calculateScore(c2, base);
        return s2.compareTo(s1);
    }
}
