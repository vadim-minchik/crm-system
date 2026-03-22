package com.studio.crm_system.config;

import com.studio.crm_system.service.CrmDataRevisionService;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * После успешного коммита транзакции, в которой вызывались save/delete репозиториев, увеличивает глобальную ревизию данных.
 */
@Aspect
@Component
public class RepositoryMutationRevisionAspect {

	private final CrmDataRevisionService revisionService;

	public RepositoryMutationRevisionAspect(CrmDataRevisionService revisionService) {
		this.revisionService = revisionService;
	}

	@AfterReturning(pointcut = "execution(* org.springframework.data.repository.CrudRepository+.save*(..))", returning = "result")
	public void afterSave(Object result) {
		scheduleBump();
	}

	@AfterReturning("execution(* org.springframework.data.repository.CrudRepository+.delete(..))")
	public void afterDelete() {
		scheduleBump();
	}

	@AfterReturning("execution(* org.springframework.data.repository.CrudRepository+.deleteById(..))")
	public void afterDeleteById() {
		scheduleBump();
	}

	@AfterReturning("execution(* org.springframework.data.repository.CrudRepository+.deleteAll(..))")
	public void afterDeleteAll() {
		scheduleBump();
	}

	@AfterReturning("execution(* org.springframework.data.repository.CrudRepository+.deleteAllById(..))")
	public void afterDeleteAllById() {
		scheduleBump();
	}

	private void scheduleBump() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					revisionService.bump();
				}
			});
		} else {
			revisionService.bump();
		}
	}
}
