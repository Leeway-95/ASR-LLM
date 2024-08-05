package discriminator;

import evaluator.TargetShapelet;
import utils.DTWM;
import utils.Tuple2;

/**
 * PSContract类用于处理主形状体（PrimaryShapelet）的合同计算。
 * 这包括计算主形状体与其二级形状体的相似度合同和二级决策函数。
 */
public class PSContract {
    /**
     * 主函数，演示了如何创建和配置主形状体和二级形状体。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SecondaryShapelet sdn1 = new SecondaryShapelet();
        sdn1.setSimilarity(0.89);
        SecondaryShapelet sdn2 = new SecondaryShapelet();
        sdn2.setSimilarity(0.95);
        PrimaryShapelet pdn = new PrimaryShapelet();
        pdn.setSimilarity(0.9);
        pdn.setSecondaryShapelet(new Tuple2<SecondaryShapelet, SecondaryShapelet>(sdn1, sdn2));
    }

    /**
     * 计算主形状体的合同值。
     * 合同值是基于主形状体和其包含的两个二级形状体的相似度差值的平均值。
     *
     * @param pdn 主形状体
     * @return 计算得到的合同值
     */
    public static Double addContract(PrimaryShapelet pdn) {
        Double pdn_simi = pdn.getSimilarity();
        Tuple2<SecondaryShapelet, SecondaryShapelet> secondaryShapelet = pdn.getSecondaryShapelet();
        SecondaryShapelet first = secondaryShapelet.getFirst();
        SecondaryShapelet second = secondaryShapelet.getSecond();
        Double sdn1_simi = 0D;
        Double sdn2_simi = 0D;
        if (first != null) {
            sdn1_simi = first.getSimilarity();
        }
        if (second != null) {
            sdn2_simi = second.getSimilarity();
        }
        Double avg_simi = (sdn1_simi + sdn2_simi) / 2;
        Double contract = Math.abs(avg_simi - pdn_simi);
        first.setContract(contract);
        second.setContract(contract);
        return contract;
    }

    /**
     * 根据目标形状体与主形状体的相似度比较，执行二级决策函数。
     * 如果目标形状体与主形状体的相似度高于某个二级形状体的相似度，
     * 则计算并返回相应的差值，否则返回0。
     *
     * @param pdn 主形状体
     * @param s   目标形状体
     * @return 根据决策函数计算得到的值，如果无返回则为0
     */
    public static Double secondaryDecision(PrimaryShapelet pdn, TargetShapelet s) {
        Double[] s_ts = s.getShapelet().getS();
        Double[] pdn_ts = pdn.getShapelet().getS();
        Double dtwm = DTWM.calculateDTWM(s_ts, pdn_ts, "FM");
        if (dtwm > pdn.getSimilarity()) {
            SecondaryShapelet second = pdn.getSecondaryShapelet().getSecond();
            Double dtwm_sec = Math.abs(dtwm - second.getSimilarity());
            if (dtwm_sec > second.getContract()) {
                return dtwm_sec;
            }
        } else {
            SecondaryShapelet first = pdn.getSecondaryShapelet().getFirst();
            Double dtwm_fir = dtwm - first.getSimilarity();
            if (dtwm_fir > first.getContract()) {
                return dtwm_fir;
            }
        }
        return 0D;
    }
}
