package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import java.time.Month;

import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

import org.junit.Before;
import org.junit.Test;

public class CrossEntryTest
{
    Client client;

    @Before
    public void createClient()
    {
        client = new Client();
        client.addAccount(new Account());
        client.addAccount(new Account());
        client.addPortfolio(new Portfolio());
        client.addPortfolio(new Portfolio());

        Security security = new Security();
        security.setName("Some security"); //$NON-NLS-1$
        client.addSecurity(security);
    }

    @Test
    public void testBuySellEntry()
    {
        Portfolio portfolio = client.getPortfolios().get(0);
        Account account = client.getAccounts().get(0);
        Security security = client.getSecurities().get(0);

        BuySellEntry entry = new BuySellEntry(portfolio, account);
        entry.setCurrencyCode(CurrencyUnit.EUR);
        entry.setDate(LocalDate.now());
        entry.setSecurity(security);
        entry.setShares(1 * Values.Share.factor());
        entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, Money.of(CurrencyUnit.EUR, 10)));
        entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, Money.of(CurrencyUnit.EUR, 11)));
        entry.setAmount(1000 * Values.Amount.factor());
        entry.setType(PortfolioTransaction.Type.BUY);
        entry.insert();

        assertThat(portfolio.getTransactions().size(), is(1));
        assertThat(account.getTransactions().size(), is(1));

        PortfolioTransaction pt = portfolio.getTransactions().get(0);
        AccountTransaction pa = account.getTransactions().get(0);

        assertThat(pt.getSecurity(), is(security));
        assertThat(pa.getSecurity(), is(security));
        assertThat(pt.getAmount(), is(pa.getAmount()));
        assertThat(pt.getDate(), is(LocalDate.now()));
        assertThat(pa.getDate(), is(LocalDate.now()));

        assertThat(pt.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 10L)));
        assertThat(pt.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, 11L)));

        // check cross entity identification
        assertThat(entry.getCrossOwner(pt), is((Object) account));
        assertThat(entry.getCrossTransaction(pt), is((Transaction) pa));

        assertThat(entry.getCrossOwner(pa), is((Object) portfolio));
        assertThat(entry.getCrossTransaction(pa), is((Transaction) pt));

        // check cross editing
        pt.setAmount(2000 * Values.Amount.factor());
        entry.updateFrom(pt);
        assertThat(pa.getAmount(), is(pt.getAmount()));

        pa.setDate(LocalDate.of(2013, Month.MARCH, 16));
        entry.updateFrom(pa);
        assertThat(pt.getDate(), is(pa.getDate()));

        // check deletion
        portfolio.deleteTransaction(pt, client);
        assertThat(portfolio.getTransactions().size(), is(0));
        assertThat(account.getTransactions().size(), is(0));
    }

    @Test
    public void testAccountTransferEntry()
    {
        Account accountA = client.getAccounts().get(0);
        Account accountB = client.getAccounts().get(1);

        AccountTransferEntry entry = new AccountTransferEntry(accountA, accountB);
        entry.setDate(LocalDate.now());
        entry.setAmount(1000 * Values.Amount.factor());
        entry.insert();

        assertThat(accountA.getTransactions().size(), is(1));
        assertThat(accountB.getTransactions().size(), is(1));

        AccountTransaction pA = accountA.getTransactions().get(0);
        AccountTransaction pB = accountB.getTransactions().get(0);

        assertThat(pA.getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertThat(pB.getType(), is(AccountTransaction.Type.TRANSFER_IN));

        assertThat(pA.getSecurity(), nullValue());
        assertThat(pB.getSecurity(), nullValue());
        assertThat(pA.getAmount(), is(pB.getAmount()));
        assertThat(pA.getDate(), is(LocalDate.now()));
        assertThat(pB.getDate(), is(LocalDate.now()));

        // check cross entity identification
        assertThat(entry.getCrossOwner(pA), is((Object) accountB));
        assertThat(entry.getCrossTransaction(pA), is((Transaction) pB));

        assertThat(entry.getCrossOwner(pB), is((Object) accountA));
        assertThat(entry.getCrossTransaction(pB), is((Transaction) pA));

        // check cross editing
        pA.setAmount(2000 * Values.Amount.factor());
        entry.updateFrom(pA);
        assertThat(pB.getAmount(), is(pA.getAmount()));

        pB.setDate(LocalDate.of(2013, Month.MARCH, 16));
        entry.updateFrom(pB);
        assertThat(pA.getDate(), is(pB.getDate()));

        // check deletion
        accountA.deleteTransaction(pA, client);
        assertThat(accountA.getTransactions().size(), is(0));
        assertThat(accountB.getTransactions().size(), is(0));
    }

    @Test
    public void testPortoflioTransferEntry()
    {
        Security security = client.getSecurities().get(0);
        Portfolio portfolioA = client.getPortfolios().get(0);
        Portfolio portfolioB = client.getPortfolios().get(1);

        PortfolioTransferEntry entry = new PortfolioTransferEntry(portfolioA, portfolioB);
        entry.setCurrencyCode(CurrencyUnit.EUR);
        entry.setDate(LocalDate.now());
        entry.setAmount(1000);
        entry.setSecurity(security);
        entry.setShares(1);
        entry.insert();

        assertThat(portfolioA.getTransactions().size(), is(1));
        assertThat(portfolioB.getTransactions().size(), is(1));

        PortfolioTransaction pA = portfolioA.getTransactions().get(0);
        PortfolioTransaction pB = portfolioB.getTransactions().get(0);

        assertThat(pA.getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(pB.getType(), is(PortfolioTransaction.Type.TRANSFER_IN));

        assertThat(pA.getSecurity(), is(security));
        assertThat(pB.getSecurity(), is(security));
        assertThat(pA.getAmount(), is(pB.getAmount()));
        assertThat(pA.getDate(), is(LocalDate.now()));
        assertThat(pB.getDate(), is(LocalDate.now()));

        // check cross entity identification
        assertThat(entry.getCrossOwner(pA), is((Object) portfolioB));
        assertThat(entry.getCrossTransaction(pA), is((Transaction) pB));

        assertThat(entry.getCrossOwner(pB), is((Object) portfolioA));
        assertThat(entry.getCrossTransaction(pB), is((Transaction) pA));

        // check cross editing
        pA.setAmount(2000);
        entry.updateFrom(pA);
        assertThat(pB.getAmount(), is(2000L));

        pA.setShares(2);
        entry.updateFrom(pA);
        assertThat(pB.getShares(), is(2L));

        pB.setDate(LocalDate.of(2013, Month.MARCH, 16));
        entry.updateFrom(pB);
        assertThat(pA.getDate(), is(pB.getDate()));

        // check deletion
        portfolioA.deleteTransaction(pA, client);
        assertThat(portfolioA.getTransactions().size(), is(0));
        assertThat(portfolioB.getTransactions().size(), is(0));
    }
}
