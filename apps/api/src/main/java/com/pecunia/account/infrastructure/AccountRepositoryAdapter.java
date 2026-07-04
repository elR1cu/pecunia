package com.pecunia.account.infrastructure;

import com.pecunia.account.application.port.out.AccountRepository;
import com.pecunia.account.domain.Account;
import com.pecunia.shared.AccountId;
import com.pecunia.shared.UserId;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountRepositoryAdapter implements AccountRepository {

    private final AccountJpaRepository accountJpaRepository;

    @Override
    public void save(Account account) {
        accountJpaRepository.save(AccountEntity.fromDomain(account));
    }

    @Override
    public Optional<Account> findByIdAndOwner(AccountId id, UserId owner) {
        return accountJpaRepository
                .findByIdAndOwnerId(id.value(), owner.value())
                .map(AccountEntity::toDomain);
    }

    @Override
    public List<Account> findAllByOwner(UserId owner) {
        return accountJpaRepository.findAllByOwnerIdOrderByCreatedAtAsc(owner.value()).stream()
                .map(AccountEntity::toDomain)
                .toList();
    }
}
