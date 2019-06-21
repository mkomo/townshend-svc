package com.mkomo.townshend.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.data.history.Revision;
import org.springframework.data.history.Revisions;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.mkomo.townshend.security.TownshendAuthentication;

@RestController
public abstract class TownshendBaseAuditController<T, ID> extends TownshendBaseController<T, ID> {

	protected abstract RevisionRepository<T, ID, Integer> getHistoryRepo();

	@RequestMapping(path = "{id}/history", method = RequestMethod.GET)
	public ResponseEntity<?> itemHistory(@PathVariable ID id, TownshendAuthentication u) {
		Revisions<Integer, T> revs = getHistoryRepo().findRevisions(id);
		List<Revision<Integer, T>> list = revs.getContent();
		ResponseEntity<?> resp = this.userCanGet(revs.getLatestRevision().getEntity(), u);
		if (resp == null) {
			return ResponseEntity.ok().body(list);
		} else {
			return resp;
		}
	}

	@RequestMapping(path = "{id}/history/{revisionNumber}", method = RequestMethod.GET)
	public ResponseEntity<?> itemRevision(@PathVariable ID id, @PathVariable Integer revisionNumber,
			TownshendAuthentication u) {
		Optional<Revision<Integer, T>> rev = getHistoryRepo().findRevision(id, revisionNumber);
		if (rev.isPresent()) {
			ResponseEntity<?> resp = this.userCanGet(rev.get().getEntity(), u);
			if (resp == null) {
				return ResponseEntity.ok().body(rev.get().getEntity());
			} else {
				return resp;
			}
		} else {
			return ResponseEntity.notFound().build();
		}
	}

}
