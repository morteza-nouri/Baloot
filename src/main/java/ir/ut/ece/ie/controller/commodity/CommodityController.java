package ir.ut.ece.ie.controller.commodity;

import ir.ut.ece.ie.domain.commodity.Commodity;
import ir.ut.ece.ie.exception.OnlineShopException;
import ir.ut.ece.ie.service.commodity.CommodityService;

import java.util.List;
import java.util.Optional;

public class CommodityController {
    private CommodityService commodityService;

    public CommodityController(CommodityService service) {
        this.commodityService = service;
    }

    public Commodity addCommodity(Commodity commodity) {
        return commodityService.addCommodity(commodity);
    }

    public Commodity getCommodityById(Long id) {
        return Optional.ofNullable(commodityService.getCommodityById(id))
                .orElseThrow(() -> new OnlineShopException("commodity not found"));
    }

    public List<Commodity> getCommodities() {
        return commodityService.getCommodities();
    }
}