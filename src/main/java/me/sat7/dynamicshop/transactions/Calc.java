package me.sat7.dynamicshop.transactions;

import me.sat7.dynamicshop.files.CustomConfig;
import me.sat7.dynamicshop.utilities.ConfigUtil;
import me.sat7.dynamicshop.utilities.ShopUtil;
import org.bukkit.configuration.file.FileConfiguration;

public final class Calc
{
    private Calc()
    {

    }

    // 특정 아이탬의 현재 가치를 계산 (다이나믹 or 고정가) (세금 반영)
    public static double getCurrentPrice(String shopName, String idx, boolean buy)
    {
        return getCurrentPrice(shopName, idx, buy, false);
    }

    public static double getCurrentPrice(String shopName, String idx, boolean buy, boolean raw)
    {
        FileConfiguration data = ShopUtil.shopConfigFiles.get(shopName).get();

        double value;
        if (!buy && data.contains(idx + ".value2"))
        {
            value = data.getDouble(idx + ".value2");
        } else
        {
            value = data.getDouble(idx + ".value");
        }

        double min = data.getDouble(idx + ".valueMin", 0.01);
        double max = data.getDouble(idx + ".valueMax");
        int median = data.getInt(idx + ".median");
        int stock = data.getInt(idx + ".stock");

        double price;
        if (median <= 0 || stock <= 0)
        {
            price = value;
        } else
        {
            if(!buy && stock < Integer.MAX_VALUE)
                stock = stock + 1;

            price = (median * value) / stock;
        }

        if (price < min)
        {
            price = min;
        }
        if (max != 0 && price > max)
        {
            price = max;
        }

        // 판매세 계산 (임의 지정된 판매가치가 없는 경우에만)
        if (!buy && !data.contains(idx + ".value2"))
        {
            double tax = ((price / 100) * getTaxRate(shopName));
            price -= tax;
        }

        if (!raw && data.contains("Options.flag.integeronly"))
        {
            if(buy)
                return Math.ceil(price);
            else
                return Math.floor(price);
        }
        else
        {
            return price;
        }
    }

    // 특정 아이탬의 앞으로 n개의 가치합을 계산 (다이나믹 or 고정가) (세금 반영)
    public static double calcTotalCost(String shopName, String idx, int amount)
    {
        FileConfiguration data = ShopUtil.shopConfigFiles.get(shopName).get();

        double total = 0;
        int median = data.getInt(idx + ".median");
        int stock = data.getInt(idx + ".stock");

        double value;
        if (amount < 0 && data.contains(idx + ".value2"))
        {
            value = data.getDouble(idx + ".value2");
        } else
        {
            value = data.getDouble(idx + ".value");
        }

        if (median <= 0 || stock <= 0)
        {
            total = value * Math.abs(amount);
        } else
        {
            for (int i = 0; i < Math.abs(amount); i++)
            {
                if (amount < 0 && stock < Integer.MAX_VALUE)
                {
                    stock++;
                }
                double temp = median * value / stock;
                double min = data.getDouble(idx + ".valueMin", 0.01);
                double max = data.getDouble(idx + ".valueMax");

                if (temp < min)
                {
                    temp = min;
                }
                if (max != 0 && temp > max)
                {
                    temp = max;
                }

                total += temp;

                if (amount > 0)
                {
                    stock--;
                    if (stock < 2)
                    {
                        break;
                    }
                }
            }
        }

        // 세금 적용 (판매가 별도지정시 세금계산 안함)
        if (amount < 0 && !data.contains(idx + ".value2"))
        {
            double tax = ((total / 100) * getTaxRate(shopName));
            total -= tax;
        }

        if (data.contains("Options.flag.integeronly"))
        {
            if(amount > 0)
                return Math.ceil(total);
            else
                return Math.floor(total);
        }
        else
        {
            return (Math.round(total * 100) / 100.0);
        }
    }

    // 상점의 세율 반환
    public static int getTaxRate(String shopName)
    {
        CustomConfig data = ShopUtil.shopConfigFiles.get(shopName);

        if (data.get().contains("Options.SalesTax"))
        {
            return data.get().getInt("Options.SalesTax");
        } else
        {
            return ConfigUtil.getCurrentTax();
        }
    }
}
